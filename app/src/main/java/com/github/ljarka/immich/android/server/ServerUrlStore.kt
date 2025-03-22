package com.github.ljarka.immich.android.server

import android.content.SharedPreferences
import androidx.core.content.edit
import com.github.ljarka.immich.android.preferences.EncryptedPreferences
import com.github.ljarka.immich.android.preferences.PreferencesStore
import javax.inject.Inject
import javax.inject.Singleton

private val SERVER_URL = "SERVER_URL"

@Singleton
class ServerUrlStore @Inject constructor(
    @EncryptedPreferences private val preferences: SharedPreferences,
) : PreferencesStore {

    var serverUrl: String = ""
        set(value) {
            preferences.edit { putString(SERVER_URL, value) }
            field = value
        }
        get() {
            return preferences.getString(SERVER_URL, "") ?: ""
        }

    override fun hasData() = serverUrl.isNotEmpty()
}