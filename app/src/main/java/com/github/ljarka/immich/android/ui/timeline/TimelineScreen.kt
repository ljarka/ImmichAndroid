package com.github.ljarka.immich.android.ui.timeline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage

@Composable
fun TimelineScreen() {
    val viewModel: TimelineViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        viewModel.fetchTimeBuckets()
    }

    val state = viewModel.state.collectAsStateWithLifecycle()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 128.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(4.dp)
    ) {
        state.value.forEach { bucket ->
            item(span = { GridItemSpan(maxLineSpan) }, key = bucket.timeBucket.hashCode()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = bucket.timeBucket)
                }
            }

            items(
                span = { index ->
                    val asset = viewModel.getAsset(
                        bucket.timeBucket,
                        index
                    )
                    if (asset == null) {
                        GridItemSpan(1)
                    } else if (asset.isPortrait) {
                        GridItemSpan(1)
                    } else {
                        GridItemSpan(3)
                    }
                },
                count = bucket.count,
                key = { index -> "${bucket.timeBucket}_$index".hashCode() }) { index ->

                val itemLifecycleOwner = remember { ItemLifecycleOwner() }
                DisposableEffect(Unit) {
                    itemLifecycleOwner.start()
                    onDispose {
                        itemLifecycleOwner.stop()
                    }
                }
                val loadingState =
                    viewModel.assetLoadingState[bucket.timeBucket]?.collectAsStateWithLifecycle(
                        lifecycleOwner = itemLifecycleOwner
                    )

                if (loadingState?.value == false) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                    )

                    LaunchedEffect(bucket.timeBucket) {
                        viewModel.fetchAsset(bucket.timeBucket)
                    }
                } else {
                    SubcomposeAsyncImage(
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .padding(4.dp)
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .clip(RoundedCornerShape(8.dp)),
                        model = viewModel.getAsset(bucket.timeBucket, index)?.url,
                        loading = {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp),
                            )
                        },
                        contentDescription = null,
                    )
                }
            }
        }
    }
}
