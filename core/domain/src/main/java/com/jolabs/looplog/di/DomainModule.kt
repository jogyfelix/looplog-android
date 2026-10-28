package com.jolabs.looplog.di

import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.data.repository.HabitRepository
import com.jolabs.looplog.data.repository.ai.AiFeatureRepository
import com.jolabs.looplog.domain.CreateHabitUseCase
import com.jolabs.looplog.ai_engine.InputLanguageDetector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {

    @Provides
    @Singleton
    fun provideCreateHabitUseCase(habitRepository: HabitRepository, analyticsTracker: AnalyticsTracker) : CreateHabitUseCase {
            return  CreateHabitUseCase(habitRepository,analyticsTracker)
    }

    @Provides
    @Singleton
    fun provideExtractHabitsUseCase(
        aiEngine: com.jolabs.looplog.ai_engine.AIEngine,
        aiFeatureRepository: AiFeatureRepository,
        inputLanguageDetector: InputLanguageDetector,
    ) : com.jolabs.looplog.domain.ExtractHabitsUseCase {
        return com.jolabs.looplog.domain.ExtractHabitsUseCase(aiEngine, aiFeatureRepository, inputLanguageDetector)
    }
}
