package com.github.ljarka.immich.android.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ImagesDao {
    @Query("SELECT * FROM assets where timestamp = :timestamp order by assetIndex")
    suspend fun getAssets(timestamp: Long): List<AssetEntity>

    @Query("SELECT * FROM assets order by timestamp asc")
    suspend fun getAllAssets(): List<AssetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssets(assets: List<AssetEntity>)

    @Delete
    suspend fun deleteAsset(asset: AssetEntity)

    @Query("SELECT * FROM month_buckets order by timestamp desc")
    fun getMonthBuckets(): Flow<List<MonthBucketEntity>>

    @Query("SELECT * FROM assets where timestamp = :timestamp and assetIndex = :assetIndex")
    suspend fun getAsset(timestamp: Long, assetIndex: Int): AssetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonthBuckets(bucket: List<MonthBucketEntity>)

    @Update
    suspend fun updateBucket(bucket: MonthBucketEntity)
}