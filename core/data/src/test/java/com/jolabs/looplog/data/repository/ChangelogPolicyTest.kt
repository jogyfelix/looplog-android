package com.jolabs.looplog.data.repository

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ChangelogPolicyTest {

    @Test
    fun `matching upgraded version that has not been shown is eligible`() {
        val eligible = shouldShowChangelog(
            installedVersionCode = 80,
            remoteVersionCode = 80,
            lastShownVersionCode = 79,
            wasUpdatedInstall = true,
        )

        assertThat(eligible).isTrue()
    }

    @Test
    fun `clean install is not eligible`() {
        val eligible = shouldShowChangelog(
            installedVersionCode = 80,
            remoteVersionCode = 80,
            lastShownVersionCode = null,
            wasUpdatedInstall = false,
        )

        assertThat(eligible).isFalse()
    }

    @Test
    fun `existing upgraded install without launch history is eligible`() {
        val eligible = shouldShowChangelog(
            installedVersionCode = 80,
            remoteVersionCode = 80,
            lastShownVersionCode = null,
            wasUpdatedInstall = true,
        )

        assertThat(eligible).isTrue()
    }

    @Test
    fun `already shown version is not eligible`() {
        val eligible = shouldShowChangelog(
            installedVersionCode = 80,
            remoteVersionCode = 80,
            lastShownVersionCode = 80,
            wasUpdatedInstall = true,
        )

        assertThat(eligible).isFalse()
    }

    @Test
    fun `remote notes for another version are not eligible`() {
        val eligible = shouldShowChangelog(
            installedVersionCode = 79,
            remoteVersionCode = 80,
            lastShownVersionCode = 78,
            wasUpdatedInstall = true,
        )

        assertThat(eligible).isFalse()
    }
}
