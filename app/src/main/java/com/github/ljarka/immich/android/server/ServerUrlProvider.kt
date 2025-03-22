package com.github.ljarka.immich.android.server

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerUrlProvider @Inject constructor() {
    var url: String? = null
}