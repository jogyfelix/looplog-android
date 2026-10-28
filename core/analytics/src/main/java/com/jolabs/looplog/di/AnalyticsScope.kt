package com.jolabs.looplog.di

import javax.inject.Qualifier

/** Application-lifetime background scope used to dispatch analytics calls. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AnalyticsScope
