package com.jolabs.looplog.storage.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class AiPreferencesMigrationTest {
    private val legacyKey = booleanPreferencesKey("ai_privacy_accepted")
    private val noticeVersionKey = intPreferencesKey("ai_notice_version")
    private val unrelatedKey = stringPreferencesKey("unrelated")
    private val migration = AiPreferencesMigration()

    @Test
    fun `legacy accepted value is removed without granting current acknowledgment`() = runTest {
        val preferences = mutablePreferencesOf(
            legacyKey to true,
            unrelatedKey to "preserved",
        )

        assertThat(migration.shouldMigrate(preferences)).isTrue()

        val migrated = migration.migrate(preferences)

        assertThat(migrated.contains(legacyKey)).isFalse()
        assertThat(migrated.contains(noticeVersionKey)).isFalse()
        assertThat(migrated[unrelatedKey]).isEqualTo("preserved")
    }

    @Test
    fun `legacy declined value is removed`() = runTest {
        val preferences = mutablePreferencesOf(legacyKey to false)

        val migrated = migration.migrate(preferences)

        assertThat(migrated.contains(legacyKey)).isFalse()
    }

    @Test
    fun `migration is skipped when legacy value is absent`() = runTest {
        val preferences = mutablePreferencesOf(noticeVersionKey to CURRENT_AI_NOTICE_VERSION)

        assertThat(migration.shouldMigrate(preferences)).isFalse()
    }

    @Test
    fun `migration preserves an existing notice version`() = runTest {
        val preferences = mutablePreferencesOf(
            legacyKey to true,
            noticeVersionKey to CURRENT_AI_NOTICE_VERSION,
        )

        val migrated = migration.migrate(preferences)

        assertThat(migrated[noticeVersionKey]).isEqualTo(CURRENT_AI_NOTICE_VERSION)
    }

    @Test
    fun `current notice version is acknowledged`() {
        val preferences = mutablePreferencesOf(noticeVersionKey to CURRENT_AI_NOTICE_VERSION)

        assertThat(preferences.hasAcknowledgedCurrentAiNotice()).isTrue()
    }

    @Test
    fun `missing or previous notice version is not acknowledged`() {
        assertThat(mutablePreferencesOf().hasAcknowledgedCurrentAiNotice()).isFalse()
        assertThat(
            mutablePreferencesOf(noticeVersionKey to CURRENT_AI_NOTICE_VERSION - 1)
                .hasAcknowledgedCurrentAiNotice()
        ).isFalse()
    }
}
