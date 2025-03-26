package com.github.ljarka.immich.android.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "assets",
    primaryKeys = ["timestamp", "assetIndex"],
    indices = [Index(value = ["timestamp", "assetIndex"])]
)
data class AssetEntity(
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "assetIndex") val assetIndex: Int,
    @ColumnInfo(name = "assetId") val assetId: String,
    @ColumnInfo(name = "width") val width: Int,
    @ColumnInfo(name = "height") val height: Int,
)