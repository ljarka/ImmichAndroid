package com.github.ljarka.immich.android.ui.timeline

import com.github.ljarka.immich.android.AppScope
import com.github.ljarka.immich.android.UrlProvider
import com.github.ljarka.immich.android.db.AssetEntity
import com.github.ljarka.immich.android.db.ImagesDatabase
import com.github.ljarka.immich.android.db.MonthBucketEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val imagesDatabase: ImagesDatabase,
    private val timelineBucketsService: TimelineBucketsService,
    private val urlProvider: UrlProvider,
) {
    private val imagesDao by lazy { imagesDatabase.imagesDao() }
    private val _timeBuckets: MutableStateFlow<Map<Long, TimeBucketUi>> =
        MutableStateFlow(emptyMap())
    val timeBuckets = _timeBuckets.asStateFlow()

    fun getAssetsCount(): Int = _timeBuckets.value.values.sumOf { it.count }

    fun getTimeBuckets(): Flow<List<TimeBucketUi>> {
        return imagesDao.getMonthBuckets()
            .map { buckets ->
                buckets.map { bucketItem ->
                    TimeBucketUi(
                        timeStamp = bucketItem.timestamp,
                        count = bucketItem.count,
                        formattedDate = formatDate(Instant.ofEpochMilli(bucketItem.timestamp)),
                        numberOfRows = bucketItem.rowsNumber ?: 0,
                    )
                }
            }.onEach {
                if (_timeBuckets.value.isEmpty()) {
                    _timeBuckets.value = it.runningFoldIndexed(
                        TimeBucketUi()
                    ) { index, acc, bucket ->
                        TimeBucketUi(
                            timeStamp = bucket.timeStamp,
                            index = acc.index + acc.count,
                            count = bucket.count,
                            formattedDate = formatDate(Instant.ofEpochMilli(bucket.timeStamp)),
                            numberOfRows = bucket.numberOfRows,
                        )
                    }.drop(1).associate {
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
            .onStart {
                val dbBuckets = imagesDao.getMonthBuckets().firstOrNull()
                val dbRowsNumbers = dbBuckets?.associate { it.timestamp to it.rowsNumber }
                val buckets = timelineBucketsService.getTimeBuckets()
                    .map {
                        MonthBucketEntity(
                            timestamp = Instant.parse(it.timeBucket).toEpochMilli(),
                            count = it.count,
                        )
                    }.map {
                        it.copy(rowsNumber = dbRowsNumbers?.get(it.timestamp))
                    }
                imagesDao.insertMonthBuckets(buckets)

                val uiBuckets = buckets.map { item ->
                    TimeBucketUi(
                        timeStamp = item.timestamp,
                        count = item.count,
                        formattedDate = formatDate(Instant.ofEpochMilli(item.timestamp)),
                        numberOfRows = item.rowsNumber,
                    )
                }
                emit(uiBuckets)
            }
    }

    fun getAsset(bucket: Long, position: Int): AssetUi? {
        return _timeBuckets.value[bucket]?.items?.getOrNull(position)
    }

    suspend fun getAsset(index: Int): AssetUi? {
        return withContext(Dispatchers.Default) {
            val sortedValues =
                _timeBuckets.value.values.sortedByDescending { it.timeStamp }
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

            val bucket = _timeBuckets.value[currentBucket.timeStamp]
            bucket?.items?.getOrNull(index - assetsBefore)
        }
    }

    suspend fun fetchAssets(bucket: Long) {
        val dbAssets = imagesDao.getAssets(bucket)

        if (dbAssets.isEmpty()) {
            val remoteAssets = timelineBucketsService.getBucket(
                timeBucket = Instant.ofEpochMilli(bucket).toString()
            )
            updateDbAssets(bucket, remoteAssets)
            remoteAssets.map {
                val ratio = calculateRatio(it.exifInfo)
                AssetUi(
                    id = it.id,
                    url = urlProvider.getThumbnail(it.id),
                    span = calculateSpan(ratio = ratio),
                )
            }
        } else {
            dbAssets.map {
                AssetUi(
                    id = it.assetId,
                    url = urlProvider.getThumbnail(it.assetId),
                    span = calculateSpan(ratio = calculateRatio(it.width ?: 1, it.height ?: 1)),
                )
            }.also {
                coroutineScope.launch(Dispatchers.IO) {
                    updateDbAssets(
                        bucket,
                        timelineBucketsService.getBucket(
                            timeBucket = Instant.ofEpochMilli(bucket).toString()
                        )
                    )
                }
            }
        }.also {
            val items = adjustSpans(it)
            val rows = (items.sumOf { it.span } + 3) / 4
            imagesDao.updateBucket(MonthBucketEntity(bucket, items.size, rows))
            _timeBuckets.value[bucket]?.items = items
        }
    }

    private suspend fun updateDbAssets(bucket: Long, assets: List<Asset>) {
        imagesDao.insertAssets(
            assets = assets.mapIndexed { index, asset ->
                AssetEntity(
                    timestamp = bucket,
                    assetIndex = index,
                    assetId = asset.id,
                    width = asset.exifInfo.exifImageWidth,
                    height = asset.exifInfo.exifImageHeight,
                )
            }
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
                _timeBuckets.value.values.sortedByDescending { it.timeStamp }
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

    private fun calculateRatio(exifInfo: ExifInfo): Float {
        return if (exifInfo.exifImageWidth == null || exifInfo.exifImageHeight == null) {
            1f
        } else {
            calculateRatio(
                exifInfo.exifImageWidth,
                exifInfo.exifImageHeight,
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