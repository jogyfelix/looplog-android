package com.jolabs.looplog.di

import com.jolabs.looplog.analytics.AnalyticsProvider
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.analytics.DefaultAnalyticsTracker
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {

    /** Allows the provider set to be empty until real backends are registered. */
    @Multibinds
    abstract fun analyticsProviders(): Set<AnalyticsProvider>

    @Binds
    @Singleton
    abstract fun bindAnalyticsTracker(impl: DefaultAnalyticsTracker): AnalyticsTracker

    companion object {
        @Provides
        @Singleton
        @AnalyticsScope
        fun provideAnalyticsScope(): CoroutineScope =
            CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
