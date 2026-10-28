package com.jolabs.looplog.di

import com.jolabs.looplog.analytics.AnalyticsProvider
import com.jolabs.looplog.analytics.FirebaseAnalyticsProvider
import com.jolabs.looplog.analytics.SentryAnalyticsProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class ProviderModule {

    @Binds
    @IntoSet
    abstract fun bindSentryProvider(impl: SentryAnalyticsProvider): AnalyticsProvider

    @Binds
    @IntoSet
    abstract fun bindFirebaseProvider(impl: FirebaseAnalyticsProvider): AnalyticsProvider
}
