package com.github.ljarka.immich.android.server

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

private val SERVER_URL = stringPreferencesKey("SERVER_URL")

@Singleton
class ServerUrlProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val url: StateFlow<String> = context.serverStore.data
        .map { preferences -> preferences[SERVER_URL] ?: "" }
        .stateIn(MainScope(), SharingStarted.Lazily, "")

    suspend fun updateUrl(url: String) {
        context.serverStore.edit { settings ->
            settings[SERVER_URL] = url
        }
    }
}