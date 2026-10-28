package com.jolabs.looplog.di

import com.jolabs.looplog.analytics.AnalyticsProvider
import com.jolabs.looplog.analytics.LogcatAnalyticsProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class DebugAnalyticsModule {

    @Binds
    @IntoSet
    abstract fun bindLogcatProvider(impl: LogcatAnalyticsProvider): AnalyticsProvider
}
