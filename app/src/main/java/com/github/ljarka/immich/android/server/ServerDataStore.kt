package com.github.ljarka.immich.android.server

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

val Context.serverStore: DataStore<Preferences> by preferencesDataStore(name = "Server")