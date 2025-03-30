package com.github.ljarka.immich.android.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ImagesDao {
    @Query("SELECT * FROM assets where bucket = :bucket order by assetIndex")
    suspend fun getAssets(bucket: Long): List<AssetEntity>

    @Query("SELECT * FROM assets order by bucket asc")
    suspend fun getAllAssets(): List<AssetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssets(assets: List<AssetEntity>)

    @Delete
    suspend fun deleteAsset(asset: AssetEntity)

    @Query("SELECT * FROM month_buckets order by timestamp desc")
    fun getMonthBuckets(): Flow<List<MonthBucketEntity>>

    @Query("SELECT * FROM assets where bucket = :bucket and assetIndex = :assetIndex")
    suspend fun getAsset(bucket: Long, assetIndex: Int): AssetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonthBuckets(bucket: List<MonthBucketEntity>)

    @Query(
        """
        UPDATE month_buckets 
        SET rowsNumber = :rowsNumber, 
            lastUpdate = :lastUpdate
        WHERE timestamp = :timestamp
    """
    )
    suspend fun updateBucket(
        timestamp: Long,
        rowsNumber: Int?,
        lastUpdate: Long = System.currentTimeMillis()
    )
}