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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced

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
    content: @Composable BoxScope.() -> Unit,
) {
    var targetOffsetX by remember { mutableFloatStateOf(0f) }
    var targetOffsetY by remember { mutableFloatStateOf(0f) }
    var targetScale by remember { mutableFloatStateOf(1f) }

    val animatedOffsetX by animateFloatAsState(targetValue = targetOffsetX, label = "offsetX")
    val animatedOffsetY by animateFloatAsState(targetValue = targetOffsetY, label = "offsetY")
    val animatedScale: Float by animateFloatAsState(targetValue = targetScale, label = "scale")

    var centerX by remember { mutableFloatStateOf(0f) }
    var centerY by remember { mutableFloatStateOf(0f) }

    val transformableState =
        rememberTransformableState { zoomChange, offsetChange, rotationChange ->
            targetScale *= zoomChange
            targetOffsetY += offsetChange.y * targetScale
            targetOffsetX += offsetChange.x * targetScale
        }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onPlaced {
                centerX = it.size.width / 2f
                centerY = it.size.height / 2f
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        if (targetScale <= ZoomStatus.Zoom1().scale) {
                            targetOffsetX += (centerX - offset.x)
                            targetOffsetY += (centerY - offset.y)
                            targetScale = ZoomStatus.Zoom2().scale
                        } else if (targetScale <= ZoomStatus.Zoom2().scale) {
                            targetOffsetX += (centerX - offset.x) * targetScale
                            targetOffsetY += (centerY - offset.y) * targetScale
                            targetScale = ZoomStatus.Zoom4().scale
                        } else {
                            targetScale = ZoomStatus.Zoom1().scale
                            targetOffsetX = 0f
                            targetOffsetY = 0f
                        }
                    })
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()

                        if (event.type == PointerEventType.Release) {
                            if (targetScale == ZoomStatus.Zoom1().scale && (targetOffsetX != 0f || targetOffsetY != 0f)) {
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
