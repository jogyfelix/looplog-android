package com.jolabs.looplog.data.repository

import com.google.common.truth.Truth.assertThat
import com.jolabs.looplog.model.AppLockMode
import com.jolabs.looplog.storage.datastore.BiometricStore
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class AppLockRepositoryImplTest {

    @Test
    fun `legacy open-only mode maps to open-and-resume mode`() = runTest {
        val biometricStore = mockk<BiometricStore> {
            every { lockMode } returns flowOf("ON_APP_OPEN")
        }

        val mode = AppLockRepositoryImpl(biometricStore).lockMode.first()

        assertThat(mode).isEqualTo(AppLockMode.ON_APP_OPEN_AND_RESUME)
    }
}
