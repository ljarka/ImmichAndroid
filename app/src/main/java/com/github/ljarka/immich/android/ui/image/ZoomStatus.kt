package com.github.ljarka.immich.android.ui.image

sealed interface ZoomStatus {
    val scale: Float

    data class Zoom1(override val scale: Float = 1f) : ZoomStatus
    data class Zoom2(override val scale: Float = 2f) : ZoomStatus
    data class Zoom4(override val scale: Float = 4f) : ZoomStatus
}