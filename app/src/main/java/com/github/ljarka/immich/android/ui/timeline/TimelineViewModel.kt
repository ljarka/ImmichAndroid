package com.github.ljarka.immich.android.ui.timeline

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.ljarka.immich.android.UrlProvider
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
    val urlProvider: UrlProvider,
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
                    val ratio = it.exifInfo.exifImageWidth.toFloat() / it.exifInfo.exifImageHeight
                    AssetUi(
                        url = urlProvider.getThumbnail(it.id),
                        id = it.id,
                        span = if (ratio >= 1.5) 4 else if (ratio >= 1.35) 3 else if (ratio >= 1) 2 else 1
                    )
                }
                val result = mutableListOf<AssetUi>()
                val spanMax = 4
                var spanSum = 0
                assets.forEach {
                    spanSum = spanSum + it.span
                    if (spanSum == spanMax) {
                        result.add(it)
                        spanSum = 0
                    } else if (spanSum < spanMax) {
                        result.add(it)
                    } else {
                        val span = it.span - (spanSum - spanMax)

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

    override fun onCleared() {
        super.onCleared()
        Log.d("lukjar", "onCleared")
    }
}