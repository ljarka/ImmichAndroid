package com.github.ljarka.immich.android.ui.image

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import kotlin.math.max

private sealed interface ZoomStatus {
    val scale: Float

    data class Zoom1(override val scale: Float = 1f) : ZoomStatus
    data class Zoom2(override val scale: Float = 2f) : ZoomStatus
    data class Zoom4(override val scale: Float = 4f) : ZoomStatus
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PinchToZoom(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    scale: Float = 1f,
    offset: Offset = Offset.Zero,
    onScaleChanged: (scale: Float) -> Unit,
    onOffsetChanged: (offset: Offset) -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    val animatedOffsetX by animateFloatAsState(targetValue = offset.x, label = "offsetX")
    val animatedOffsetY by animateFloatAsState(targetValue = offset.y, label = "offsetY")
    val animatedScale: Float by animateFloatAsState(targetValue = scale, label = "scale")

    var centerX by remember { mutableFloatStateOf(0f) }
    var centerY by remember { mutableFloatStateOf(0f) }

    val transformableState =
        rememberTransformableState { zoomChange, offsetChange, rotationChange ->
            onScaleChanged(
                max(
                    ZoomStatus.Zoom1().scale,
                    minOf(scale * zoomChange, ZoomStatus.Zoom4().scale)
                )
            )

            val offsetX = calculateOffset(offset.x, offsetChange.x, centerX, scale)
            val offsetY = calculateOffset(offset.y, offsetChange.y, centerY, scale)

            onOffsetChanged(
                Offset(
                    x = offsetX,
                    y = offsetY,
                )
            )
        }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onPlaced {
                centerX = it.size.width / 2f
                centerY = it.size.height / 2f
            }
            .pointerInput(scale, offset) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        if (scale <= ZoomStatus.Zoom1().scale) {
                            onOffsetChanged(
                                Offset(
                                    x = (centerX - offset.x) * scale,
                                    y = (centerY - offset.y) * scale,
                                )
                            )
                            onScaleChanged(ZoomStatus.Zoom2().scale)
                        } else if (scale <= ZoomStatus.Zoom2().scale) {
                            onOffsetChanged(
                                Offset(
                                    x = (centerX - offset.x) * scale,
                                    y = (centerY - offset.y) * scale,
                                )
                            )
                            onScaleChanged(ZoomStatus.Zoom4().scale)
                        } else {
                            onScaleChanged(ZoomStatus.Zoom1().scale)
                            onOffsetChanged(Offset.Zero)
                        }
                    })
            }
            .pointerInput(scale, offset) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()

                        if (event.type == PointerEventType.Release) {
                            if (scale == ZoomStatus.Zoom1().scale && (offset.x != 0f || offset.y != 0f)) {
                                onDismissRequest()
                            }
                        }
                    }
                }
            }

            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                translationX = animatedOffsetX
                translationY = animatedOffsetY
            }
            .transformable(state = transformableState),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

private fun calculateOffset(value: Float, change: Float, center: Float, scale: Float): Float {
    val newValue = value + change * scale
    return if (value < 0) {
        maxOf(newValue, -center * scale)
    } else {
        minOf(newValue, center * scale)
    }
}
