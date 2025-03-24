package com.github.ljarka.immich.android.ui.image

import androidx.lifecycle.ViewModel
import com.github.ljarka.immich.android.UrlProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ImageDetailsViewModel @Inject constructor(
    private val urlProvider: UrlProvider,
) : ViewModel() {

    fun getThumbnail(assetId: String): String = urlProvider.getThumbnail(assetId)

    fun getPreview(assetId: String): String = urlProvider.getPreview(assetId)
}