package com.jolabs.looplog.data.repository

import com.jolabs.looplog.model.update.UpdateConfig

interface UpdateRepository {
    suspend fun getUpdateConfig(): UpdateConfig

    suspend fun getGraceStartedAt(): Long?
    suspend fun setGraceStartedAt(timestamp: Long)
    suspend fun clearGraceStarted()

    fun isOnline(): Boolean
}
