package com.github.ljarka.immich.android.server

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ServerConfigurationViewModel @Inject constructor(
    private val serverUrlProvider: ServerUrlProvider
) : ViewModel() {

    fun setServerUrl(url: String) {
        serverUrlProvider.url = url
    }
}