package com.github.ljarka.immich.android.ui.timeline

import android.content.Context
import com.github.ljarka.immich.android.AppScope
import com.github.ljarka.immich.android.UrlProvider
import com.github.ljarka.immich.android.db.AssetEntity
import com.github.ljarka.immich.android.db.AssetType
import com.github.ljarka.immich.android.db.ImagesDatabase
import com.github.ljarka.immich.android.db.MonthBucketEntity
import com.github.ljarka.immich.android.local.LocalImagesProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class AssetIndex(
    val index: Int,
    val assetId: String,
)

@Singleton
class TimelineRepository @Inject constructor(
    @AppScope val coroutineScope: CoroutineScope,
    @ApplicationContext private val context: Context,
    private val imagesDatabase: ImagesDatabase,
    private val timelineBucketsService: TimelineBucketsService,
    private val urlProvider: UrlProvider,
) {
    private val timeBucketsCache: MutableStateFlow<Map<Long, TimeBucketUi>> =
        MutableStateFlow(emptyMap())
    private val imagesDao by lazy { imagesDatabase.imagesDao() }

    fun getAssetsCount(): Int = timeBucketsCache.value.values.sumOf { it.count }

    fun getTimeBuckets(): Flow<List<TimeBucketUi>> {
        return loadBucketsFromDb().onStart {
            imagesDao.insertMonthBuckets(loadBucketsInfoFromServerAndDisk())
        }
    }

    private suspend fun loadBucketsInfoFromServerAndDisk(): List<MonthBucketEntity> {
        val localImagesCount = LocalImagesProvider().getImageCountForCurrentMonth(context)
        val currentTime = Instant.now().atZone(ZoneId.systemDefault())
        val dbBuckets = imagesDao.getMonthBuckets().firstOrNull()
        val dbRowsNumbers = dbBuckets?.associate { it.timestamp to it.rowsNumber }
        return timelineBucketsService.getTimeBuckets()
            .map {
                val instant = Instant.parse(it.timeBucket)
                val bucketTime = instant.atZone(ZoneId.systemDefault())

                val count =
                    if (bucketTime.year == currentTime.year && bucketTime.month == currentTime.month) {
                        it.count + localImagesCount
                    } else {
                        it.count
                    }
                bucketTime.month
                bucketTime.year
                val timestamp = instant.toEpochMilli()

                MonthBucketEntity(
                    timestamp = timestamp,
                    count = count,
                    rowsNumber = dbRowsNumbers?.get(timestamp)
                )
            }.runningFoldIndexed(
                MonthBucketEntity()
            ) { index, acc, bucket ->
                MonthBucketEntity(
                    timestamp = bucket.timestamp,
                    index = acc.index + acc.count,
                    count = bucket.count,
                    rowsNumber = bucket.rowsNumber,
                )
            }.drop(1)
    }

    private fun loadBucketsFromDb() = imagesDao.getMonthBuckets()
        .map { buckets ->
            buckets.map { bucketItem ->
                TimeBucketUi(
                    timeStamp = bucketItem.timestamp,
                    count = bucketItem.count,
                    formattedDate = formatDate(Instant.ofEpochMilli(bucketItem.timestamp)),
                    numberOfRows = bucketItem.rowsNumber ?: 0,
                    index = bucketItem.index,
                )
            }
        }.onEach {
            if (timeBucketsCache.value.isEmpty()) {
                timeBucketsCache.value = it.associate {
                    it.timeStamp to TimeBucketUi(
                        timeStamp = it.timeStamp,
                        count = it.count,
                        formattedDate = it.formattedDate,
                        index = it.index,
                        numberOfRows = it.numberOfRows,
                    )
                }
            }
        }

    fun getAsset(bucket: Long, position: Int): AssetUi? {
        return timeBucketsCache.value[bucket]?.items?.getOrNull(position)
    }

    suspend fun getAsset(index: Int): AssetUi? {
        return withContext(Dispatchers.Default) {
            val sortedValues =
                timeBucketsCache.value.values.sortedByDescending { it.timeStamp }
            var itemsCount = 0
            val bucketsBefore = sortedValues.takeWhile {
                itemsCount += it.count
                itemsCount < index
            }
            val assetsBefore = bucketsBefore.sumOf { it.count }
            val currentBucketIndex = bucketsBefore.size
            val currentBucket = sortedValues[currentBucketIndex]

            if (currentBucket.items.isEmpty()) {
                fetchAssets(currentBucket.timeStamp)
            }

            val bucket = timeBucketsCache.value[currentBucket.timeStamp]
            bucket?.items?.getOrNull(index - assetsBefore)
        }
    }

    suspend fun fetchAssets(bucket: Long) {
        val dbAssets = imagesDao.getAssets(bucket)
        if (dbAssets.isEmpty()) {
            updateAssets(bucket)
            imagesDao.getAssets(bucket)
                .map {
                    AssetUi(
                        id = it.assetId,
                        url = urlProvider.getThumbnail(it.assetId, it.type),
                        span = calculateSpan(ratio = calculateRatio(it.width ?: 1, it.height ?: 1)),
                        type = it.type,
                    )
                }
        } else {
            dbAssets.map {
                AssetUi(
                    id = it.assetId,
                    url = urlProvider.getThumbnail(it.assetId, it.type),
                    span = calculateSpan(ratio = calculateRatio(it.width ?: 1, it.height ?: 1)),
                    type = it.type,
                )
            }.also {
                coroutineScope.launch(Dispatchers.IO) {
                    updateAssets(bucket)
                }
            }
        }.also {
            val items = adjustSpans(it)
            val rows = (items.sumOf { it.span } + 3) / 4
            imagesDao.updateBucket(MonthBucketEntity(bucket, items.size, rows))
            timeBucketsCache.value[bucket]?.items = items
        }
    }

    private suspend fun updateAssets(bucket: Long) {
        val timeInstant = Instant.ofEpochMilli(bucket)

        val remoteAssets = timelineBucketsService.getBucket(
            timeBucket = timeInstant.toString()
        ).map {
            AssetEntity(
                timestamp = bucket,
                assetId = it.id,
                width = it.exifInfo.exifImageWidth,
                height = it.exifInfo.exifImageHeight,
                dateTaken = Instant.parse(it.exifInfo.dateTimeOriginal).toEpochMilli(),
                assetIndex = 0,
                type = AssetType.REMOTE,
            )
        }

        val zoned = timeInstant.atZone(ZoneId.systemDefault())
        val localAssets = LocalImagesProvider()
            .getImagesForMonth(context, zoned.month.ordinal, zoned.year)
            .map {
                AssetEntity(
                    timestamp = bucket,
                    assetId = it.id.toString(),
                    width = it.width,
                    height = it.height,
                    type = AssetType.LOCAL,
                    dateTaken = it.dateTaken,
                    assetIndex = 0,
                )
            }

        updateDbAssets(remoteAssets + localAssets)
    }

    private suspend fun updateDbAssets(assets: List<AssetEntity>) {
        imagesDao.insertAssets(
            assets = assets
                .sortedByDescending { it.dateTaken }
                .mapIndexed { index, asset -> asset.copy(assetIndex = index) }
        )
    }

    private fun formatDate(instant: Instant): String {
        val date = instant.atZone(ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("MMMM, yyyy", Locale.ENGLISH)
        return date.format(formatter)
    }

    private fun calculateSpan(ratio: Float) =
        if (ratio >= 1.5) 4 else if (ratio >= 1.35) 3 else if (ratio >= 1) 2 else 1

    suspend fun getIndexOfAsset(assetId: String): AssetIndex {
        return withContext(Dispatchers.Default) {
            val sortedValues =
                timeBucketsCache.value.values.sortedByDescending { it.timeStamp }
            val bucketsBefore = sortedValues.takeWhile {
                it.items.indexOfFirst { it.id == assetId } == -1
            }
            val currentBucketIndex = bucketsBefore.size
            val numberOfItemsInBucketsBefore = bucketsBefore.sumOf { it.count }

            val bucket = sortedValues[currentBucketIndex]
            val indexInBucket = bucket.items.indexOfFirst { it.id == assetId }
            AssetIndex(
                index = numberOfItemsInBucketsBefore + indexInBucket,
                assetId = assetId,
            )
        }
    }

    private fun adjustSpans(assets: List<AssetUi>): List<AssetUi> {
        val result = mutableListOf<AssetUi>()
        val spanMax = 4
        var spanSum = 0
        assets.forEach {
            spanSum = spanSum + it.span
            if (spanSum == spanMax) {
                result.add(it)
                spanSum = 0
            } else if (spanSum < spanMax) {
                result.add(it)
            } else {
                val span = it.span - (spanSum - spanMax)

                if (span == 0) {
                    result.add(it.copy(span = 1))
                    spanSum = 1
                } else {
                    result.add(it.copy(span = span))
                    spanSum = 0
                }
            }
        }
        return result
    }

    private fun calculateRatio(with: Int, height: Int): Float {
        return with.toFloat() / height.toFloat()
    }
}