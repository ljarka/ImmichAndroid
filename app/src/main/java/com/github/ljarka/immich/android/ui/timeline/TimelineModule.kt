package com.github.ljarka.immich.android.ui.timeline

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
class TimelineModule {

    @Provides
    fun timelineBucketService(retrofit: retrofit2.Retrofit) =
        retrofit.create(TimelineBucketsService::class.java)
}