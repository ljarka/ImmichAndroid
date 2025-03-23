package com.github.ljarka.immich.android.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.ljarka.immich.android.ui.server.ServerUrlStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel @Inject constructor(
    val timelineRepository: TimelineRepository,
    val serverUrlStore: ServerUrlStore,
) : ViewModel() {

    private val bucketsCache = mutableMapOf<String, List<AssetUi>>()

    private val _state = MutableStateFlow<List<TimeBucket>>(emptyList())
    val state = _state.asStateFlow()

    val assetLoadingState = mutableMapOf<String, MutableStateFlow<Boolean>>()

    private val assetFetchingJobs = mutableMapOf<String, Job>()

    fun fetchTimeBuckets() {
        viewModelScope.launch {
            val timeBuckets = timelineRepository.getTimeBuckets()
            timeBuckets.forEach {
                assetLoadingState[it.timeBucket] = MutableStateFlow(false)
            }
            _state.value = timeBuckets
        }
    }

    fun getAsset(bucket: String, position: Int): AssetUi? {
        return bucketsCache[bucket]?.getOrNull(position)
    }

    fun fetchAsset(bucket: String) {
        val job = assetFetchingJobs[bucket]

        if (job == null || !job.isActive) {
            assetFetchingJobs[bucket] = viewModelScope.launch {
                val assets = timelineRepository.getAssets(bucket)
                bucketsCache[bucket] =
                    assets.map {
                        AssetUi(
                            buildImageUrl(it.id),
                            isPortrait = it.exifInfo.exifImageWidth < it.exifInfo.exifImageHeight
                        )
                    }
                assetLoadingState[bucket]?.value = true
            }
        }
    }

    private fun buildImageUrl(assetId: String): String {
        return "${serverUrlStore.serverUrl}/api/assets/${assetId}/thumbnail"
    }
}