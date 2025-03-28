package com.github.ljarka.immich.android.ui.timeline

import android.content.res.Configuration.ORIENTATION_PORTRAIT
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.enterAlwaysScrollBehavior
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import com.github.ljarka.immich.android.R
import kotlinx.coroutines.launch

enum class BaseRowType {
    SINGLE, DOUBLE, QUADRUPLE
}

data class CalculatedRows(
    val singleItemRowCount: Int = 0,
    val doubleItemRowCount: Int = 0,
    val quadrupleItemRowCount: Int = 0,
)

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = enterAlwaysScrollBehavior()
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Hidden,
            skipHiddenState = false,
        )
    )

    LaunchedEffect(Unit) {
        scaffoldState.bottomSheetState.expand()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val viewModel: TimelineViewModel = hiltViewModel()
        val state = viewModel.state.collectAsStateWithLifecycle()
        val gridState = rememberLazyGridState()

        BottomSheetScaffold(
            sheetPeekHeight = 0.dp,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            scaffoldState = scaffoldState,
            sheetContent = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(96.dp)
                ) {
                    IconTextButton(icon = Icons.Default.Delete, text = "Delete", onClick = {

                    })
                }
            },
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
            val orientation = LocalConfiguration.current.orientation

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
                        count = bucket.count,
                        span = { index ->
                            val defaultSpan = GridItemSpan(2)
                            if (viewModel.isFixedSpan(bucket.timeStamp)) {
                                defaultSpan
                            } else {
                                val item = viewModel.getAsset(bucket.timeStamp, index)
                                calculateSpan(index, item, rows) ?: defaultSpan.also {
                                    viewModel.setFixedSpan(bucket.timeStamp)
                                }
                            }
                        }, key = { index -> "${bucket.timeStamp}_$index".hashCode() }) { index ->

                        val itemLifecycleOwner = remember { ItemLifecycleOwner() }
                        DisposableEffect(Unit) {
                            itemLifecycleOwner.start()
                            onDispose {
                                itemLifecycleOwner.stop()
                            }
                        }
                        val asset = viewModel.getAsset(bucket.timeStamp, index)
                        val assetLoading by viewModel.getAssetLoadingState(bucket.timeStamp)
                            .collectAsStateWithLifecycle(lifecycleOwner = itemLifecycleOwner)

                        if (asset == null) {
                            if (assetLoading != AssetLoadingState.LOADING) {
                                LaunchedEffect(bucket.timeStamp) {
                                    viewModel.fetchAssets(bucket.timeStamp)
                                }
                            }
                            PlaceHolder(modifier = Modifier.padding(4.dp))
                        } else {
                            GalleryItem(
                                asset = asset,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                onImageClick = onImageClick
                            )
                        }
                    }

                }
            }
        }

        if (state.value.isNotEmpty()) {
            FastScrollComponent(
                buckets = state.value,
                gridState = gridState,
            )
        }
    }
}

private fun calculateSpan(
    index: Int,
    item: AssetUi?,
    rows: CalculatedRows?,
): GridItemSpan? {
    return if (item != null) {
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
        null
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

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
private fun GalleryItem(
    asset: AssetUi,
    onImageClick: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    with(sharedTransitionScope) {
        var isInEditMode by remember { mutableStateOf(false) }
        val scale = remember { Animatable(1f) }
        val coroutineScope = rememberCoroutineScope()
        SubcomposeAsyncImage(
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .scale(scale.value)
                .sharedElement(
                    state = rememberSharedContentState(key = asset.id),
                    animatedVisibilityScope = animatedVisibilityScope,
                )
                .combinedClickable(
                    onClick = {
                        if (isInEditMode) {
                            if (scale.value == 0.9f) {
                                coroutineScope.launch {
                                    scale.animateTo(1f)
                                }
                            } else {
                                coroutineScope.launch {
                                    scale.animateTo(0.9f)
                                }
                            }
                        } else {
                            onImageClick(asset.id)
                        }
                    },
                    onLongClick = {
                        isInEditMode = true
                        coroutineScope.launch {
                            scale.animateTo(0.9f)
                        }
                    },
                )
                .padding(4.dp)
                .height(170.dp)
                .clip(RoundedCornerShape(8.dp)),
            model = asset.url,
            loading = { PlaceHolder() },
            contentDescription = null,
        )
    }
}

@Composable
private fun PlaceHolder(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier
            .height(170.dp)
            .fillMaxWidth()
            .background(
                color = Color.LightGray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
    )
}

@Composable
fun IconTextButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 34.dp),
                onClick = onClick
            )
            .padding(8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = text)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text)
    }
}
