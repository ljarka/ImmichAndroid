package com.github.ljarka.immich.android.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AssetState {
    LOADING, LOADED, DEFAULT
}

@HiltViewModel
class TimelineViewModel @Inject constructor(
    val timelineRepository: TimelineRepository,
) : ViewModel() {

    private val _assetState = mutableMapOf<Long, MutableStateFlow<AssetState>>()
    val timeBuckets = timelineRepository.timeBuckets

    fun getAssetLoadingState(bucket: Long): StateFlow<AssetState> {
        return _assetState.getOrPut(bucket) { MutableStateFlow(AssetState.DEFAULT) }
    }

    val state = timelineRepository.getTimeBuckets()
        .stateIn(viewModelScope, started = SharingStarted.WhileSubscribed(5_000), emptyList())

    private val assetFetchingJobs = mutableMapOf<Long, Job>()

    fun getAsset(bucket: Long, position: Int): AssetUi? {
        return timelineRepository.getAsset(bucket, position)
    }

    fun fetchAsset(bucket: Long) {
        val job = assetFetchingJobs[bucket]

        if (job == null || !job.isActive) {
            assetFetchingJobs[bucket] = viewModelScope.launch(Dispatchers.IO) {
                _assetState[bucket]?.value = AssetState.LOADING
                timelineRepository.fetchAssets(bucket)
                _assetState[bucket]?.value = AssetState.LOADED
            }
        }
    }
}