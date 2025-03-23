package com.github.ljarka.immich.android.ui.timeline

import kotlinx.serialization.Serializable

@Serializable
data class TimeBucket(
    val timeBucket: String,
    val count: Int,
)