package com.jolabs.looplog.ai_engine.di

import com.jolabs.looplog.ai_engine.AIEngine
import com.jolabs.looplog.ai_engine.DefaultAIEngine
import com.jolabs.looplog.ai_engine.InputLanguageDetector
import com.jolabs.looplog.ai_engine.MlKitInputLanguageDetector
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AIModule {

    @Binds
    @Singleton
    abstract fun bindAIEngine(defaultAIEngine: DefaultAIEngine): AIEngine

    @Binds
    @Singleton
    abstract fun bindInputLanguageDetector(detector: MlKitInputLanguageDetector): InputLanguageDetector
}
