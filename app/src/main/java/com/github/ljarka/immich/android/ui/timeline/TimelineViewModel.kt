package com.github.ljarka.immich.android.ui.timeline

import android.util.LruCache
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

enum class AssetLoadingState {
    LOADING, LOADED, DEFAULT
}

@HiltViewModel
class TimelineViewModel @Inject constructor(
    val timelineRepository: TimelineRepository,
) : ViewModel() {
    private val loadingState = MutableStateFlow(LoadingState.DEFAULT)
    val state = combine(timelineRepository.getTimeBuckets(), loadingState) { items, loadingState ->
        BucketsState(
            loadingState = loadingState,
            items = items,
        )
    }.stateIn(viewModelScope, started = SharingStarted.WhileSubscribed(5_000), BucketsState())
    private val fixedSpans = mutableSetOf<Long>()
    private val _assetState = ConcurrentHashMap<Long, MutableStateFlow<AssetLoadingState>>()
    private val assetFetchingJobs = object : LruCache<Long, Job>(5) {
        override fun entryRemoved(evicted: Boolean, key: Long?, oldValue: Job?, newValue: Job?) {
            oldValue?.cancel()
        }
    }
    private val _selectedAssets = MutableStateFlow<Set<String>>(emptySet())
    val selectedAssets: StateFlow<Set<String>> = _selectedAssets.asStateFlow()

    fun getAssetLoadingState(bucket: Long): StateFlow<AssetLoadingState> {
        return _assetState.getOrPut(bucket) { MutableStateFlow(AssetLoadingState.DEFAULT) }
    }

    fun isFixedSpan(bucket: Long): Boolean = fixedSpans.contains(bucket)

    fun setFixedSpan(bucket: Long) {
        fixedSpans.add(bucket)
    }

    fun getAsset(bucket: Long, position: Int): AssetUi? {
        return timelineRepository.getAsset(bucket, position)
    }

    @Synchronized
    fun fetchAssets(bucket: Long) {
        val job = assetFetchingJobs[bucket]

        if (job == null) {
            assetFetchingJobs.put(bucket, viewModelScope.launch(Dispatchers.IO) {
                val loadingState = _assetState[bucket]
                loadingState?.value = AssetLoadingState.LOADING
                val fetched = timelineRepository.fetchAssets(bucket)

                if (fetched) {
                    loadingState?.value = AssetLoadingState.LOADED
                } else {
                    loadingState?.value = AssetLoadingState.DEFAULT
                }
                assetFetchingJobs.remove(bucket)
            })
        }
    }

    fun selectAsset(assetId: String) {
        _selectedAssets.update {
            it.toMutableSet().apply {
                add(assetId)
            }
        }
    }

    fun deselectAsset(assetId: String) {
        _selectedAssets.update {
            it.toMutableSet().apply {
                remove(assetId)
            }
        }
    }

    fun clearSelection() {
        _selectedAssets.value = emptySet()
    }

    fun refresh() {
        viewModelScope.launch {
            loadingState.value = LoadingState.REFRESHING
            timelineRepository.refresh()
            loadingState.value = LoadingState.LOADED
        }
    }
}