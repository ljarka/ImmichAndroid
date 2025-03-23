package com.github.ljarka.immich.android.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.ljarka.immich.android.ui.server.ServerUrlStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel @Inject constructor(
    val timelineRepository: TimelineRepository,
    val serverUrlStore: ServerUrlStore,
) : ViewModel() {

    private val bucketsCache = mutableMapOf<String, List<AssetUi>>()
    val assetLoadingState = mutableMapOf<String, MutableStateFlow<Boolean>>()

    val state = flow {
        val timeBuckets = timelineRepository.getTimeBuckets()
            .map { TimeBucketUi(it.timeBucket, it.count, formatDate(it.timeBucket)) }
        timeBuckets.forEach {
            assetLoadingState[it.timeBucket] = MutableStateFlow(false)
        }
        emit(timeBuckets)
    }.stateIn(viewModelScope, started = SharingStarted.WhileSubscribed(5_000), emptyList())

    private val assetFetchingJobs = mutableMapOf<String, Job>()

    fun getAsset(bucket: String, position: Int): AssetUi? {
        return bucketsCache[bucket]?.getOrNull(position)
    }

    fun fetchAsset(bucket: String) {
        val job = assetFetchingJobs[bucket]

        if (job == null || !job.isActive) {
            assetFetchingJobs[bucket] = viewModelScope.launch(Dispatchers.IO) {
                val assets = timelineRepository.getAssets(bucket).map {
                    AssetUi(
                        buildImageUrl(it.id),
                        span = if (it.exifInfo.exifImageWidth < it.exifInfo.exifImageHeight) 1 else 3
                    )
                }
                val result = mutableListOf<AssetUi>()
                var spanSum = 0
                assets.forEach {
                    spanSum = spanSum + it.span
                    if (spanSum == 3) {
                        result.add(it)
                        spanSum = 0
                    } else if (spanSum < 3) {
                        result.add(it)
                    } else {
                        val span = it.span - (spanSum - 3)

                        if (span == 0) {
                            result.add(it.copy(span = 1))
                            spanSum = 1
                        } else {
                            result.add(it.copy(span = span))
                            spanSum = 0
                        }
                    }
                }

                bucketsCache[bucket] = result
                assetLoadingState[bucket]?.value = true
            }
        }
    }

    private fun formatDate(date: String): String {
        val date = OffsetDateTime.parse(date)
        val formatter = DateTimeFormatter.ofPattern("MMMM, yyyy", Locale.ENGLISH)
        return date.format(formatter)
    }

    private fun buildImageUrl(assetId: String): String {
        return "${serverUrlStore.serverUrl}/api/assets/${assetId}/thumbnail"
    }
}