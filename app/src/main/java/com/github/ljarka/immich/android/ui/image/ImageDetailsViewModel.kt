package com.github.ljarka.immich.android.ui.image

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.ljarka.immich.android.UrlProvider
import com.github.ljarka.immich.android.db.AssetType
import com.github.ljarka.immich.android.ui.timeline.AssetIndex
import com.github.ljarka.immich.android.ui.timeline.TimelineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImageDetailsViewModel @Inject constructor(
    private val urlProvider: UrlProvider,
    private val repository: TimelineRepository,
) : ViewModel() {

    private val _initialPage = MutableStateFlow<AssetIndex?>(null)
    val initialPage = _initialPage.asStateFlow()

    fun checkInitialPage(assetId: String) {
        viewModelScope.launch {
            _initialPage.value = repository.getIndexOfAsset(assetId)
        }
    }

    fun getNumberOfPages(): Int = repository.getAssetsCount()

    fun getAssetId(index: Int): Flow<String> = flow {
        if (initialPage.value?.index == index) {
            emit(_initialPage.value?.assetId ?: "")
        } else {
            emit(repository.getAsset(index)?.id ?: "")
        }
    }

    fun getPreview(assetId: String): String = urlProvider.getPreview(assetId, AssetType.REMOTE)
    fun getThumbnail(assetId: String): String = urlProvider.getThumbnail(assetId, AssetType.REMOTE)
}