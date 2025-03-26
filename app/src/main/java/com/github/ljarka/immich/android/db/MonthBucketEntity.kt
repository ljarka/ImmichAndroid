package com.github.ljarka.immich.android.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "month_buckets",
    primaryKeys = ["timestamp"],
    indices = [Index(value = ["timestamp"])]
)
data class MonthBucketEntity(
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "count") val count: Int,
)