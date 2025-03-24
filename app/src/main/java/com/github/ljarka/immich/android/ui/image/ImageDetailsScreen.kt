package com.github.ljarka.immich.android.ui.image

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ImageDetailsScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onDismissRequest: () -> Unit,
    assetId: String,
) {
    BackHandler(enabled = sharedTransitionScope.isTransitionActive) {
        // do nothing
    }

    val viewModel = hiltViewModel<ImageDetailsViewModel>()
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
                            .clickable {
                                if (!sharedTransitionScope.isTransitionActive) {
                                    onDismissRequest()
                                }
                            },
                    )
                },
            )
        }
    ) { innerPadding ->
        with(sharedTransitionScope) {
            PinchToZoom(
                modifier = Modifier.padding(innerPadding),
                onDismissRequest = {
                    if (!sharedTransitionScope.isTransitionActive) {
                        onDismissRequest()
                    }
                }
            ) {
                SubcomposeAsyncImage(
                    modifier = Modifier
                        .fillMaxWidth()
                        .sharedElement(
                            rememberSharedContentState(key = assetId),
                            animatedVisibilityScope = animatedVisibilityScope,
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
