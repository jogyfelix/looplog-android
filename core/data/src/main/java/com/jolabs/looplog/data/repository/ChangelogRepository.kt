package com.jolabs.looplog.data.repository

import com.jolabs.looplog.model.changelog.Changelog

interface ChangelogRepository {
    suspend fun hasPendingAutomaticChangelog(): Boolean
    suspend fun getCurrentChangelog(): Changelog?
    suspend fun shouldShowAutomatically(changelog: Changelog): Boolean
    suspend fun markShown(versionCode: Long)
}
