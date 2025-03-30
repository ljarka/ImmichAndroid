package com.github.ljarka.immich.android.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

enum class AssetType {
    LOCAL, REMOTE
}

@Entity(
    tableName = "assets",
    primaryKeys = ["bucket", "assetIndex"],
    indices = [Index(value = ["bucket", "assetIndex"])]
)
data class AssetEntity(
    @ColumnInfo(name = "bucket") val timestamp: Long,
    @ColumnInfo(name = "assetIndex") val assetIndex: Int,
    @ColumnInfo(name = "assetId") val assetId: String,
    @ColumnInfo(name = "width") val width: Int?,
    @ColumnInfo(name = "height") val height: Int?,
    @ColumnInfo(name = "type") val type: AssetType,
    @ColumnInfo(name = "dateTaken") val dateTaken: Long? = null
)