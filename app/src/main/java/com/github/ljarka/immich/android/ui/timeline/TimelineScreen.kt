package com.github.ljarka.immich.android.ui.timeline

import android.annotation.SuppressLint
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.util.Log
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.enterAlwaysScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import com.github.ljarka.immich.android.R
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class BaseRowType {
    SINGLE, DOUBLE, QUADRUPLE
}

data class CalculatedRows(
    val singleItemRowCount: Int = 0,
    val doubleItemRowCount: Int = 0,
    val quadrupleItemRowCount: Int = 0,
)

private val fixedSpans = mutableSetOf<Long>()

@SuppressLint("UnusedBoxWithConstraintsScope")
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
        val gridState = rememberLazyGridState()

        LazyVerticalGrid(
            columns = if (orientation == ORIENTATION_PORTRAIT) GridCells.Fixed(4) else GridCells.Fixed(
                8
            ),
            state = gridState,
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

                val rows: CalculatedRows? =
                    if (bucket.numberOfRows != null && bucket.numberOfRows != 0) calculateRowsSizes(
                        bucket.numberOfRows,
                        bucket.count
                    ) else null

                items(
                    count = bucket.count, span = { index ->
                        val item = viewModel.getAsset(bucket.timeStamp, index)
                        if (fixedSpans.contains(bucket.timeStamp)) {
                            GridItemSpan(2)
                        } else if (item != null) {
                            GridItemSpan(item.span)
                        } else if (rows != null) {
                            if (index + 1 < rows.singleItemRowCount) {
                                GridItemSpan(4)
                            } else if (index + 1 < rows.singleItemRowCount + rows.doubleItemRowCount) {
                                GridItemSpan(2)
                            } else if (index + 1 < rows.singleItemRowCount + rows.doubleItemRowCount + rows.quadrupleItemRowCount) {
                                GridItemSpan(1)
                            } else {
                                GridItemSpan(2)
                            }
                        } else {
                            fixedSpans.add(bucket.timeStamp)
                            GridItemSpan(2)
                        }
                    }, key = { index -> "${bucket.timeStamp}_$index".hashCode() }) { index ->

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
        val buckets = viewModel.timeBuckets.collectAsStateWithLifecycle()

        if (buckets.value.values.isNotEmpty()) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
                val topPadding = remember { innerPadding.calculateTopPadding() }
                val bottomPadding = remember { innerPadding.calculateBottomPadding() }
                var numberOfItems = buckets.value.keys.size
                var maxOffset = with(LocalDensity.current) {
                    (maxHeight - topPadding - bottomPadding - 42.dp).toPx()
                }
                var minDelta = maxOffset / numberOfItems
                var offsetY by rememberSaveable { mutableStateOf(0) }
                var index by rememberSaveable { mutableStateOf(0) }
                var coroutineScope = rememberCoroutineScope()
                var text by remember { mutableStateOf(buckets.value.values.first().formattedDate) }

                LaunchedEffect(gridState) {
                    snapshotFlow { gridState.firstVisibleItemIndex }
                        .collect { firstItemIndex ->
                            val bucket =
                                buckets.value.values.lastOrNull { it.index <= firstItemIndex }
                            if (bucket != null) {
                                text = bucket.formattedDate
                            }
                        }
                }

                val draggableState = rememberDraggableState(onDelta = { delta ->
                    offsetY = if (delta < 0) {
                        maxOf(offsetY + minOf(delta.roundToInt(), minDelta.toInt()), 0)
                    } else {
                        minOf(
                            offsetY + maxOf(delta.roundToInt(), minDelta.toInt()),
                            maxOffset.roundToInt()
                        )
                    }

                    index = minOf(
                        maxOf((offsetY / minDelta).roundToInt(), 0),
                        buckets.value.values.size - 1
                    )

                    coroutineScope.launch {
                        gridState.scrollToItem(buckets.value.values.toList()[index].index + index)
                        text = buckets.value.values.toList()[index].formattedDate
                    }
                })
                Button(
                    modifier = Modifier
                        .padding(top = topPadding)
                        .offset { IntOffset(0, offsetY) }
                        .align(Alignment.TopEnd)
                        .draggable(
                            state = draggableState,
                            orientation = Orientation.Vertical
                        ),
                    onClick = {}

                ) {
                    Text(text = text)
                }
            }
        }
    }
}

private fun calculateRowsSizes(numberOfRows: Int, itemsCount: Int): CalculatedRows {
    var singleItemRows = itemsCount // number of rows equal number of items
    val doubleItemsRows = itemsCount / 2
    val quadrupleItemRows = itemsCount / 4

    val baseRowType = if (singleItemRows - numberOfRows <= 0) {
        BaseRowType.SINGLE
    } else if (doubleItemsRows - numberOfRows <= 0) {
        BaseRowType.DOUBLE
    } else {
        BaseRowType.QUADRUPLE
    }

    return when (baseRowType) {
        BaseRowType.SINGLE -> CalculatedRows(singleItemRowCount = singleItemRows)
        BaseRowType.DOUBLE -> {
            val difference = numberOfRows - doubleItemsRows

            CalculatedRows(
                singleItemRowCount = difference * 2,
                doubleItemRowCount = doubleItemsRows - difference
            )
        }

        BaseRowType.QUADRUPLE -> {
            val difference = numberOfRows - quadrupleItemRows
            val quadrupleRows = quadrupleItemRows - difference
            val doubleRows = difference * 2
            val singleRows =
                maxOf(itemsCount - (quadrupleRows * 4 + doubleRows * 2), 0)

            CalculatedRows(
                singleItemRowCount = singleRows,
                quadrupleItemRowCount = quadrupleItemRows - difference,
                doubleItemRowCount = difference * 2,
            )
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
