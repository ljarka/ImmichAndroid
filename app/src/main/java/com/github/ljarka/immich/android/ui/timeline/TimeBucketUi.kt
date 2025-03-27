package com.github.ljarka.immich.android.ui.timeline

data class TimeBucketUi(
    val timeStamp: Long = 0,
    val count: Int = 0,
    val formattedDate: String = "",
    val index: Int = 0,
    var items: List<AssetUi> = emptyList(),
    var spans: List<Int> = emptyList(),
)