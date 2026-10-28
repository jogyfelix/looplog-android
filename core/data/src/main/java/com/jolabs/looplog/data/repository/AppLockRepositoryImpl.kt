package com.jolabs.looplog.data.repository

import com.jolabs.looplog.model.AppLockMode
import com.jolabs.looplog.storage.datastore.BiometricStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AppLockRepositoryImpl @Inject constructor(
    private val biometricStore: BiometricStore
) : AppLockRepository {

    override val lockMode: Flow<AppLockMode> = biometricStore.lockMode.map { name ->
        when (name) {
            // Preserve app-lock protection for preferences and backups created before the
            // open-only mode was consolidated into the stricter open-and-resume mode.
            "ON_APP_OPEN" -> AppLockMode.ON_APP_OPEN_AND_RESUME
            else -> name?.let { runCatching { AppLockMode.valueOf(it) }.getOrNull() }
                ?: AppLockMode.DISABLED
        }
    }

    override suspend fun setLockMode(mode: AppLockMode) {
        biometricStore.setLockMode(mode.name)
    }
}
