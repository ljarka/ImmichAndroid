package com.github.ljarka.immich.android.ui.timeline

import retrofit2.http.GET
import retrofit2.http.Query

interface TimelineBucketsService {

    @GET("/api/timeline/buckets")
    suspend fun getTimeBuckets(
        @Query("size") size: String = "MONTH",
        @Query("withPartners") withPartners: Boolean = true,
        @Query("isArchived") isArchived: Boolean = false,
    ): List<TimeBucket>

    @GET("/api/timeline/bucket")
    suspend fun getBucket(
        @Query("size") size: String,
        @Query("timeBucket") timeBucket: String,
        @Query("withPartners") withPartners: Boolean = true,
        @Query("isArchived") isArchived: Boolean = false,
    ): List<Asset>

}