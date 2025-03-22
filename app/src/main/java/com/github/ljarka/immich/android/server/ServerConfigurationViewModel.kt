package com.github.ljarka.immich.android.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServerConfigurationViewModel @Inject constructor(
    private val serverUrlProvider: ServerUrlProvider
) : ViewModel() {

    val state = serverUrlProvider.url

    fun setServerUrl(url: String) {
        viewModelScope.launch {
            serverUrlProvider.updateUrl(url)
        }
    }
}