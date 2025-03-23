package com.github.ljarka.immich.android.network

import com.github.ljarka.immich.android.ui.server.ServerUrlStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {

    @Provides
    fun json() = Json { ignoreUnknownKeys = true }

    @Provides
    fun okhttp(authInterceptor: AuthInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(authInterceptor)
            .build()
        return client
    }

    @Provides
    fun retrofit(
        okHttpClient: OkHttpClient,
        serverUrlStore: ServerUrlStore,
        json: Json,
    ) = Retrofit.Builder()
        .baseUrl(serverUrlStore.serverUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}