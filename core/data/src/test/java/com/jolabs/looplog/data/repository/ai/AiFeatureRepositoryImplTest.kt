package com.jolabs.looplog.data.repository.ai

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.google.common.truth.Truth.assertThat
import com.jolabs.looplog.ai_engine.AIEngine
import com.jolabs.looplog.ai_engine.EngineState
import com.jolabs.looplog.model.aiDataModels.AvailableModel
import com.jolabs.looplog.storage.datastore.AiPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AiFeatureRepositoryImplTest {
    private val aiPreferences: AiPreferences = mockk(relaxed = true)
    private val aiEngine: AIEngine = mockk(relaxed = true)
    private val featureFlagProvider: AiFeatureFlagProvider = mockk()
    private val context: Context = mockk()
    private val packageManager: PackageManager = mockk()
    private val packageInfo: PackageInfo = mockk()
    private val engineState = MutableStateFlow<EngineState>(EngineState.Uninitialized)

    private lateinit var repository: AiFeatureRepositoryImpl

    @BeforeEach
    fun setUp() {
        every { context.packageName } returns "com.jolabs.looplog"
        every { context.packageManager } returns packageManager
        every { packageManager.getPackageInfo("com.jolabs.looplog", 0) } returns packageInfo
        every { packageInfo.longVersionCode } returns APP_VERSION_CODE
        every { aiEngine.state } returns engineState
        every { aiEngine.release() } just runs
        coEvery { aiPreferences.isModelPackIncompatible(APP_VERSION_CODE) } returns false
        coEvery { aiEngine.availableModel() } returns AvailableModel.LITE_RT
        coEvery { aiEngine.hasIncompatibleModelPack() } returns false

        repository = AiFeatureRepositoryImpl(
            aiPreferences = aiPreferences,
            aiEngine = aiEngine,
            featureFlagProvider = featureFlagProvider,
            context = context,
        )
    }

    @Test
    fun `enabled flag and supported hardware make AI available`() = runTest {
        coEvery { featureFlagProvider.refreshAndIsEnabled() } returns true
        coEvery { aiEngine.isAvailable() } returns true

        val result = repository.refreshAvailability()

        assertThat(result).isEqualTo(AiFeatureAvailability.Available)
        coVerify(exactly = 0) { aiEngine.removeDownloadedModel() }
    }

    @Test
    fun `disabled flag hides AI without probing hardware and removes model`() = runTest {
        coEvery { featureFlagProvider.refreshAndIsEnabled() } returns false

        val result = repository.refreshAvailability()

        assertThat(result).isEqualTo(AiFeatureAvailability.Unavailable)
        coVerify(exactly = 0) { aiEngine.isAvailable() }
        verify(exactly = 1) { aiEngine.release() }
        coVerify(exactly = 1) { aiEngine.removeDownloadedModel() }
    }

    @Test
    fun `unsupported hardware hides AI and removes model`() = runTest {
        coEvery { featureFlagProvider.refreshAndIsEnabled() } returns true
        coEvery { aiEngine.isAvailable() } returns false

        val result = repository.refreshAvailability()

        assertThat(result).isEqualTo(AiFeatureAvailability.Unavailable)
        coVerify(exactly = 1) { aiEngine.removeDownloadedModel() }
    }

    @Test
    fun `targeted pack without model is persisted as incompatible and removed`() = runTest {
        coEvery { featureFlagProvider.refreshAndIsEnabled() } returns true
        coEvery { aiEngine.isAvailable() } returns true
        coEvery { aiEngine.hasIncompatibleModelPack() } returns true

        val result = repository.refreshAvailability()

        assertThat(result).isEqualTo(AiFeatureAvailability.Unavailable)
        coVerify(exactly = 1) { aiPreferences.markModelPackIncompatible(APP_VERSION_CODE) }
        coVerify(exactly = 1) { aiEngine.removeDownloadedModel() }
    }

    @Test
    fun `incompatibility marker from current app version keeps AI unavailable`() = runTest {
        coEvery { featureFlagProvider.refreshAndIsEnabled() } returns true
        coEvery { aiEngine.isAvailable() } returns true
        coEvery { aiPreferences.isModelPackIncompatible(APP_VERSION_CODE) } returns true

        val result = repository.refreshAvailability()

        assertThat(result).isEqualTo(AiFeatureAvailability.Unavailable)
        coVerify(exactly = 1) { aiEngine.removeDownloadedModel() }
    }

    @Test
    fun `LiteRT incompatibility marker does not hide newly available Gemini Nano`() = runTest {
        coEvery { featureFlagProvider.refreshAndIsEnabled() } returns true
        coEvery { aiEngine.isAvailable() } returns true
        coEvery { aiEngine.availableModel() } returns AvailableModel.GEMINI_NANO
        coEvery { aiPreferences.isModelPackIncompatible(APP_VERSION_CODE) } returns true

        val result = repository.refreshAvailability()

        assertThat(result).isEqualTo(AiFeatureAvailability.Available)
        coVerify(exactly = 0) { aiEngine.removeDownloadedModel() }
    }

    private companion object {
        const val APP_VERSION_CODE = 75L
    }
}
