package com.github.ljarka.immich.android.ui.timeline

import retrofit2.http.GET
import retrofit2.http.Query

interface TimelineBucketsService {

    @GET("/api/timeline/buckets?size=MONTH")
    suspend fun getTimeBuckets(): List<TimeBucket>

    @GET("/api/timeline/bucket")
    suspend fun getBucket(
        @Query("size") size: String,
        @Query("timeBucket") timeBucket: String
    ): List<Asset>

}