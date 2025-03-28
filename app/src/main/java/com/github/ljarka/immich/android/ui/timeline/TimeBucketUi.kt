package com.github.ljarka.immich.android.ui.timeline

data class TimeBucketUi(
    val timeStamp: Long = 0,
    val count: Int = 0,
    val formattedDate: String = "",
    val index: Int = 0,
    val numberOfRows: Int? = null,
    var items: List<AssetUi> = emptyList(),
)