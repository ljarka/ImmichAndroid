package com.github.ljarka.immich.android.ui.timeline

import android.content.res.Configuration.ORIENTATION_PORTRAIT
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.enterAlwaysScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import com.github.ljarka.immich.android.R

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Timeline")
                },
                navigationIcon = {
                    Image(
                        painter = painterResource(R.drawable.immich_logo),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(32.dp),
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->
        val viewModel: TimelineViewModel = hiltViewModel()
        val state = viewModel.state.collectAsStateWithLifecycle()
        val orientation = LocalConfiguration.current.orientation

        LazyVerticalGrid(
            columns = if (orientation == ORIENTATION_PORTRAIT) GridCells.Fixed(4) else GridCells.Fixed(
                8
            ),
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(4.dp)
        ) {
            state.value.forEach { bucket ->
                item(span = { GridItemSpan(maxLineSpan) }, key = bucket.timeStamp) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(horizontal = 8.dp, vertical = 16.dp),
                    ) {
                        Text(
                            text = bucket.formattedDate,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                }

                items(
                    span = { index ->
                        val asset = viewModel.getAsset(
                            bucket.timeStamp,
                            index
                        )
                        if (asset == null) {
                            if (index % 5 == 0) {
                                GridItemSpan(4)
                            } else {
                                GridItemSpan(1)
                            }
                        } else {
                            GridItemSpan(asset.span)
                        }
                    },
                    count = bucket.count,
                    key = { index -> "${bucket.timeStamp}_$index".hashCode() }) { index ->

                    GalleryItem(
                        timeBucket = bucket.timeStamp,
                        index = index,
                        viewModel = viewModel,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onImageClick = onImageClick
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun GalleryItem(
    timeBucket: Long,
    index: Int,
    viewModel: TimelineViewModel,
    onImageClick: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val itemLifecycleOwner = remember { ItemLifecycleOwner() }
    DisposableEffect(Unit) {
        itemLifecycleOwner.start()
        onDispose {
            itemLifecycleOwner.stop()
        }
    }
    val assetLoading = viewModel.getAssetLoadingState(timeBucket).collectAsStateWithLifecycle(
        lifecycleOwner = itemLifecycleOwner
    )
    val asset = viewModel.getAsset(timeBucket, index)

    if (assetLoading.value == AssetState.DEFAULT && asset == null) {
        LaunchedEffect(timeBucket) { viewModel.fetchAsset(timeBucket) }
        PlaceHolder(modifier = Modifier.padding(4.dp))
    } else if (asset != null) {
        with(sharedTransitionScope) {
            SubcomposeAsyncImage(
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .sharedElement(
                        state = rememberSharedContentState(key = asset.id),
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
                    .padding(4.dp)
                    .height(170.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = { onImageClick(asset.id) }),
                model = asset.url,
                loading = { PlaceHolder() },
                contentDescription = null,
            )
        }
    } else {
        PlaceHolder(Modifier.padding(4.dp))
    }
}

@Composable
private fun PlaceHolder(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier
            .height(170.dp)
            .fillMaxWidth()
            .background(
                Color.LightGray.copy(alpha = 0.3f),
                RoundedCornerShape(8.dp)
            )
    )
}
