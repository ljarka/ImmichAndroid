package com.github.ljarka.immich.android.ui.timeline

import kotlinx.serialization.Serializable

@Serializable
data class Asset(
    val id: String,
    val exifInfo: ExifInfo,
)

@Serializable
data class ExifInfo(
    val orientation: String?,
    val exifImageWidth: Int,
    val exifImageHeight: Int,
)