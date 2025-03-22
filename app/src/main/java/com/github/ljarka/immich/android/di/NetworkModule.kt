package com.github.ljarka.immich.android.di

import com.github.ljarka.immich.android.server.ServerUrlStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(ViewModelComponent::class)
class NetworkModule {

    @Provides
    fun okhttp(): OkHttpClient {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(Level.BODY)
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
        return client
    }

    @Provides
    fun retrofit(okHttpClient: OkHttpClient, serverUrlStore: ServerUrlStore) =
        Retrofit.Builder()
            .baseUrl(serverUrlStore.serverUrl)
            .client(okHttpClient)
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .build()
}