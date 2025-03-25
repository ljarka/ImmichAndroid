package com.github.ljarka.immich.android.ui.image

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ImageDetailsScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onDismissRequest: () -> Unit,
    assetId: String,
) {
    var coroutineScope = rememberCoroutineScope()
    var offset by remember { mutableStateOf(Offset.Zero) }
    var scale by remember { mutableFloatStateOf(1f) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    fun dismiss() = coroutineScope.launch {
        if (!sharedTransitionScope.isTransitionActive) {
            if (scale > 1f) {
                scale = 1f
                offset = Offset.Zero
                delay(200)
            }
            onDismissRequest()
        }
    }
    BackHandler(enabled = true) { dismiss() }

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {},
                navigationIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(32.dp)
                            .clickable { dismiss() },
                    )
                },
            )
        }
    ) { innerPadding ->
        with(sharedTransitionScope) {
            var initialPage = 0
            val pagerState = rememberPagerState(
                pageCount = {
                    10
                },
                initialPage = initialPage
            )

            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 3,
                userScrollEnabled = scale == 1f,
                pageSpacing = 16.dp,
                modifier = Modifier
                    .pointerInput(imageSize) {
                        detectTransformGestures { centroid: Offset, pan: Offset, zoom: Float, rotation: Float ->
                            val offsetX =
                                calculateOffset(offset.x, pan.x, imageSize.width / 2f, scale)
                            val offsetY =
                                calculateOffset(offset.y, pan.y, imageSize.height / 2f, scale)
                            offset = Offset(
                                x = offsetX,
                                y = offsetY,
                            )
                            scale = max(
                                ZoomStatus.Zoom1().scale,
                                minOf(scale * zoom, ZoomStatus.Zoom4().scale)
                            )
                        }
                    }
            ) { page ->

                val isCurrentPage = page == pagerState.currentPage

                if (!(scale > 1f && !isCurrentPage)) {
                    Page(
                        modifier = Modifier.padding(innerPadding),
                        isCurrentPage = isCurrentPage,
                        assetId = assetId,
                        scale = scale,
                        offset = offset,
                        dismissRequest = { dismiss() },
                        onScaleChanged = { scale = it },
                        onOffsetChanged = { offset = it },
                        onImageSizeChanged = { imageSize = it },
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.Page(
    modifier: Modifier = Modifier,
    isCurrentPage: Boolean,
    assetId: String,
    scale: Float,
    offset: Offset,
    onScaleChanged: (scale: Float) -> Unit,
    onOffsetChanged: (offset: Offset) -> Unit,
    onImageSizeChanged: (size: IntSize) -> Unit,
    dismissRequest: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val viewModel = hiltViewModel<ImageDetailsViewModel>()
    PinchToZoom(
        modifier = modifier,
        onScaleChanged = onScaleChanged,
        onOffsetChanged = onOffsetChanged,
        scale = if (isCurrentPage) scale else 1f,
        offset = if (isCurrentPage) offset else Offset.Zero,
        onActionUp = { scale, offset ->
            if ((abs(offset.x) > 0 || abs(offset.y) > 0) && scale == ZoomStatus.Zoom1().scale && isCurrentPage) {
                dismissRequest()
            }
        }
    ) {
        SubcomposeAsyncImage(
            modifier = Modifier
                .fillMaxWidth()
                .onPlaced { onImageSizeChanged(it.size) }
                .then(
                    if (isCurrentPage) Modifier.sharedElement(
                        state = rememberSharedContentState(key = assetId),
                        animatedVisibilityScope = animatedVisibilityScope,
                    ) else Modifier
                )
                .clip(RoundedCornerShape(8.dp)),
            model = viewModel.getPreview(assetId),
            contentDescription = null,
            loading = {
                AsyncImage(
                    modifier = Modifier
                        .fillMaxWidth(),
                    model = viewModel.getThumbnail(assetId),
                    contentDescription = null,
                )
            }
        )
    }
}

private fun calculateOffset(value: Float, change: Float, center: Float, scale: Float): Float {
    val newValue = value + change
    return if (value < 0) {
        maxOf(newValue, -center * scale)
    } else {
        minOf(newValue, center * scale)
    }
}
