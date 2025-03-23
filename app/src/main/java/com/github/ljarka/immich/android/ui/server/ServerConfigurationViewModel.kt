package com.github.ljarka.immich.android.ui.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServerConfigurationViewModel @Inject constructor(
    private val serverUrlStore: ServerUrlStore
) : ViewModel() {

    fun setServerUrl(url: String) {
        viewModelScope.launch {
            serverUrlStore.serverUrl = url
        }
    }
}