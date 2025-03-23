package com.github.ljarka.immich.android.network

import com.github.ljarka.immich.android.ui.login.AccessTokenStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Provider

class AuthInterceptor @Inject constructor(
    private val tokenProvider: Provider<AccessTokenStore>
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        if (originalRequest.url.encodedPath.contains("/login")) {
            return chain.proceed(originalRequest)
        }
        val token = tokenProvider.get().accessToken
        val request = chain.request().newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(request)
    }
}