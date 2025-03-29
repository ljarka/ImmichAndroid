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

enum class AssetLoadingState {
    LOADING, LOADED, DEFAULT
}

@HiltViewModel
class TimelineViewModel @Inject constructor(
    val timelineRepository: TimelineRepository,
) : ViewModel() {

    private val fixedSpans = mutableSetOf<Long>()
    private val _assetState = mutableMapOf<Long, MutableStateFlow<AssetLoadingState>>()

    fun getAssetLoadingState(bucket: Long): StateFlow<AssetLoadingState> {
        return _assetState.getOrPut(bucket) { MutableStateFlow(AssetLoadingState.DEFAULT) }
    }

    fun isFixedSpan(bucket: Long): Boolean = fixedSpans.contains(bucket)

    fun setFixedSpan(bucket: Long) {
        fixedSpans.add(bucket)
    }

    val state = timelineRepository.getTimeBuckets()
        .stateIn(viewModelScope, started = SharingStarted.WhileSubscribed(5_000), emptyList())

    private val assetFetchingJobs = mutableMapOf<Long, Job>()

    fun getAsset(bucket: Long, position: Int): AssetUi? {
        return timelineRepository.getAsset(bucket, position)
    }

    fun fetchAssets(bucket: Long) {
        val job = assetFetchingJobs[bucket]

        if (job == null || !job.isActive) {
            assetFetchingJobs[bucket] = viewModelScope.launch(Dispatchers.IO) {
                _assetState[bucket]?.value = AssetLoadingState.LOADING
                timelineRepository.fetchAssets(bucket)
                _assetState[bucket]?.value = AssetLoadingState.LOADED
            }
        }
    }
}