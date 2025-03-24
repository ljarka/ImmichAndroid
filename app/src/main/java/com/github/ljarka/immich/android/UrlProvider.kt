package com.github.ljarka.immich.android

import com.github.ljarka.immich.android.ui.server.ServerUrlStore
import javax.inject.Inject

class UrlProvider @Inject constructor(
    private val serverUrlStore: ServerUrlStore,
) {

    fun getPreview(assetId: String): String {
        return "${serverUrlStore.serverUrl}/api/assets/$assetId/thumbnail?size=preview"
    }

    fun getThumbnail(assetId: String): String {
        return "${serverUrlStore.serverUrl}/api/assets/${assetId}/thumbnail"
    }
}