package com.github.ljarka.immich.android.ui.image

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ImageDetailsScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onDismissRequest: () -> Unit,
    assetId: String,
) {
    val viewModel = hiltViewModel<ImageDetailsViewModel>()
    with(sharedTransitionScope) {
        PinchToZoom(onDismissRequest = onDismissRequest) {
            SubcomposeAsyncImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .sharedElement(
                        rememberSharedContentState(key = assetId),
                        animatedVisibilityScope = animatedVisibilityScope,
                    ),
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