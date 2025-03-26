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

data class BucketDescriptor(
    val timeBucket: TimeBucketUi,
    var items: List<AssetUi>,
)

@Singleton
class TimelineRepository @Inject constructor(
    @AppScope val coroutineScope: CoroutineScope,
    private val imagesDatabase: ImagesDatabase,
    private val timelineBucketsService: TimelineBucketsService,
    private val urlProvider: UrlProvider,
) {
    private val imagesDao by lazy { imagesDatabase.imagesDao() }
    private val _timeBuckets: MutableStateFlow<Map<Long, BucketDescriptor>> =
        MutableStateFlow(emptyMap())
    val timeBuckets = _timeBuckets.asStateFlow()

    fun getAssetsCount(): Int = _timeBuckets.value.values.sumOf { it.timeBucket.count }

    fun getTimeBuckets(): Flow<List<TimeBucketUi>> {
        return imagesDao.getMonthBuckets()
            .map {
                it.map {
                    TimeBucketUi(
                        timeStamp = it.timestamp,
                        count = it.count,
                        formattedDate = formatDate(Instant.ofEpochMilli(it.timestamp))
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
                            formattedDate = formatDate(Instant.ofEpochMilli(bucket.timeStamp))
                        )
                    }.drop(1).associate {
                        it.timeStamp to BucketDescriptor(
                            TimeBucketUi(
                                timeStamp = it.timeStamp,
                                count = it.count,
                                formattedDate = it.formattedDate,
                                index = it.index,
                            ), emptyList()
                        )
                    }
                }
            }
            .onStart {
                val buckets = timelineBucketsService.getTimeBuckets()
                    .map {
                        MonthBucketEntity(
                            timestamp = Instant.parse(it.timeBucket).toEpochMilli(),
                            count = it.count,
                        )
                    }
                imagesDao.insertMonthBuckets(
                    buckets
                )
            }
    }

    fun getAsset(bucket: Long, position: Int): AssetUi? {
        return _timeBuckets.value[bucket]?.items?.getOrNull(position)
    }

    suspend fun getAsset(index: Int): AssetUi? {
        return withContext(Dispatchers.Default) {
            val sortedValues =
                _timeBuckets.value.values.sortedByDescending { it.timeBucket.timeStamp }
            var itemsCount = 0
            val bucketsBefore = sortedValues.takeWhile {
                itemsCount += it.timeBucket.count
                itemsCount < index
            }
            val assetsBefore = bucketsBefore.sumOf { it.timeBucket.count }
            val currentBucketIndex = bucketsBefore.size
            val currentBucket = sortedValues[currentBucketIndex]

            if (currentBucket.items.isEmpty()) {
                fetchAssets(currentBucket.timeBucket.timeStamp)
            }

            val bucket = _timeBuckets.value[currentBucket.timeBucket.timeStamp]
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
            _timeBuckets.value[bucket]?.items = adjustSpans(it)
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
                _timeBuckets.value.values.sortedByDescending { it.timeBucket.timeStamp }
            val bucketsBefore = sortedValues.takeWhile {
                it.items.indexOfFirst { it.id == assetId } == -1
            }
            val currentBucketIndex = bucketsBefore.size
            val numberOfItemsInBucketsBefore = bucketsBefore.sumOf { it.timeBucket.count }

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

    private fun calculateRatio(with: Int, height: Int): Float {
        return with.toFloat() / height.toFloat()
    }
}