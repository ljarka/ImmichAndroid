package com.github.ljarka.immich.android.ui.image

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import kotlinx.coroutines.delay

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ImageDetailsScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onDismissRequest: () -> Unit,
    assetId: String,
) {
    var dismissAction by remember { mutableStateOf(false) }
    BackHandler(enabled = true) { dismissAction = true }

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
                            .clickable { dismissAction = true },
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
            var isZoomedIn by remember { mutableStateOf(false) }
            var offset by remember { mutableStateOf(Offset.Zero) }

            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 3,
                userScrollEnabled = !isZoomedIn,
                pageSpacing = 16.dp,
                modifier = Modifier.pointerInput(Unit) {
                    detectDragGestures(onDrag = { _, dragAmount ->
                        offset = offset + dragAmount
                    })
                }
            ) { page ->
                var scale by remember { mutableFloatStateOf(1f) }
                val viewModel = hiltViewModel<ImageDetailsViewModel>()

                suspend fun dismiss() {
                    if (!sharedTransitionScope.isTransitionActive) {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                            delay(200)
                        }
                        onDismissRequest()
                    }
                }

                LaunchedEffect(dismissAction) {
                    if (dismissAction && page == pagerState.currentPage) {
                        dismiss()
                    }
                }
                LaunchedEffect(scale) {
                    isZoomedIn = scale > 1f
                }

                if (!(isZoomedIn && page != pagerState.currentPage)) {
                    PinchToZoom(
                        modifier = Modifier.padding(innerPadding),
                        onScaleChanged = { scale = it },
                        onOffsetChanged = { offset = it },
                        draggingEnabled = isZoomedIn,
                        scale = scale,
                        offset = if (page == pagerState.currentPage) offset else Offset.Zero,
                        onActionUp = { scale, offset ->
                            if ((offset.x > 0 || offset.y > 0) && scale == ZoomStatus.Zoom1().scale) {
                                dismissAction = true
                            }
                        }
                    ) {
                        SubcomposeAsyncImage(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (page == pagerState.currentPage) Modifier.sharedElement(
                                        rememberSharedContentState(key = assetId),
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
            }
        }
    }
}
