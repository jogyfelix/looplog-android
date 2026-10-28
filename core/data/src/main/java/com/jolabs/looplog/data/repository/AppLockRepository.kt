package com.jolabs.looplog.data.repository

import com.jolabs.looplog.model.AppLockMode
import kotlinx.coroutines.flow.Flow

interface AppLockRepository {
    /** The current app-lock mode, defaulting to [AppLockMode.DISABLED] when unset. */
    val lockMode: Flow<AppLockMode>

    suspend fun setLockMode(mode: AppLockMode)
}
