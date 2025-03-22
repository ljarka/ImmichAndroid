package com.github.ljarka.immich.android.init

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.ljarka.immich.android.login.AccessTokenStore
import com.github.ljarka.immich.android.server.ServerUrlStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InitViewModel @Inject constructor(
    private val serverUrlStore: ServerUrlStore,
    private val accessTokenStore: AccessTokenStore,
) : ViewModel() {

    private val initUpdates = MutableSharedFlow<InitState>()

    val initState = merge(initUpdates, flow {
        emit(checkInitState())
    }).stateIn(viewModelScope, SharingStarted.Lazily, InitState.UNKNOWN)

    fun updateState() {
        viewModelScope.launch {
            initUpdates.emit(checkInitState())
        }
    }

    private fun checkInitState() = if (accessTokenStore.hasData()) {
        InitState.INITIALIZED
    } else if (serverUrlStore.hasData()) {
        InitState.REQUIRES_LOGIN
    } else {
        InitState.REQUIRES_SERVER_CONFIGURATION
    }
}