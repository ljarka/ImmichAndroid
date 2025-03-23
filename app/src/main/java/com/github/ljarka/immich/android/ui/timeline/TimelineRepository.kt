package com.github.ljarka.immich.android.ui.timeline

import javax.inject.Inject

class TimelineRepository @Inject constructor(
    private val timelineBucketsService: TimelineBucketsService
) {
    suspend fun getTimeBuckets(): List<TimeBucket> = timelineBucketsService.getTimeBuckets()

    suspend fun getAssets(bucket: String): List<Asset> = timelineBucketsService.getBucket(
        size = "MONTH",
        timeBucket = bucket
    )
}