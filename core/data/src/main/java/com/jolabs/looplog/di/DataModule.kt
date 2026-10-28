package com.jolabs.looplog.di

import android.content.Context
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.data.backup.BackupCrypto
import com.jolabs.looplog.data.repository.AppLockRepository
import com.jolabs.looplog.data.repository.AppLockRepositoryImpl
import com.jolabs.looplog.data.repository.AppReviewRepository
import com.jolabs.looplog.data.repository.BackupRepository
import com.jolabs.looplog.data.repository.BackupRepositoryImpl
import com.jolabs.looplog.data.repository.ChangelogRepository
import com.jolabs.looplog.data.repository.ChangelogRepositoryImpl
import com.jolabs.looplog.data.repository.AppReviewRepositoryImpl
import com.jolabs.looplog.data.repository.HabitRepository
import com.jolabs.looplog.data.repository.HabitRepositoryImpl
import com.jolabs.looplog.data.repository.SettingsRepository
import com.jolabs.looplog.data.repository.SettingsRepositoryImpl
import com.jolabs.looplog.data.repository.ThemeRepository
import com.jolabs.looplog.data.repository.ThemeRepositoryImpl
import com.jolabs.looplog.data.repository.UpdateRepository
import com.jolabs.looplog.data.repository.UpdateRepositoryImpl
import com.jolabs.looplog.data.repository.ai.AiFeatureRepository
import com.jolabs.looplog.data.repository.ai.AiFeatureRepositoryImpl
import com.jolabs.looplog.data.repository.ai.AiFeatureFlagProvider
import com.jolabs.looplog.data.repository.ai.FirebaseAiFeatureFlagProvider
import com.jolabs.looplog.storage.dao.HabitDao
import com.jolabs.looplog.storage.datastore.AppReviewStore
import com.jolabs.looplog.storage.datastore.BiometricStore
import com.jolabs.looplog.storage.datastore.ThemeStore
import com.jolabs.looplog.storage.datastore.UpdateGraceStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindChangelogRepository(
        changelogRepositoryImpl: ChangelogRepositoryImpl
    ): ChangelogRepository

    @Binds
    @Singleton
    abstract fun bindAiFeatureRepository(
        aiFeatureRepositoryImpl: AiFeatureRepositoryImpl
    ): AiFeatureRepository

    @Binds
    @Singleton
    abstract fun bindAiFeatureFlagProvider(
        provider: FirebaseAiFeatureFlagProvider
    ): AiFeatureFlagProvider

    companion object {
        @Provides
        @Singleton
        fun provideHabitRepository(
            habitDao: HabitDao,
            analyticsTracker: AnalyticsTracker
        ): HabitRepository {
            return HabitRepositoryImpl(habitDao, analyticsTracker)
        }

        @Provides
        @Singleton
        fun provideUpdateRepository(
            @ApplicationContext context: Context,
            updateGraceStore: UpdateGraceStore,
            analyticsTracker: AnalyticsTracker
        ): UpdateRepository {
            return UpdateRepositoryImpl(context, updateGraceStore, analyticsTracker)
        }

        @Provides
        @Singleton
        fun provideAppReviewRepository(
            appReviewStore: AppReviewStore,
            analyticsTracker: AnalyticsTracker
        ): AppReviewRepository {
            return AppReviewRepositoryImpl(appReviewStore, analyticsTracker)
        }

        @Provides
        @Singleton
        fun provideAppLockRepository(
            biometricStore: BiometricStore
        ): AppLockRepository {
            return AppLockRepositoryImpl(biometricStore)
        }

        @Provides
        @Singleton
        fun provideThemeRepository(
            themeStore: ThemeStore
        ): ThemeRepository {
            return ThemeRepositoryImpl(themeStore)
        }

        @Provides
        @Singleton
        fun provideBackupRepository(
            @ApplicationContext context: Context,
            habitDao: HabitDao,
            themeStore: ThemeStore,
            biometricStore: BiometricStore,
            crypto: BackupCrypto,
            analyticsTracker: AnalyticsTracker
        ): BackupRepository {
            return BackupRepositoryImpl(
                context,
                habitDao,
                themeStore,
                biometricStore,
                crypto,
                analyticsTracker
            )
        }
    }
}
