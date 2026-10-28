package com.jolabs.looplog.data.repository

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ChangelogParserTest {

    @Test
    fun `valid matching payload selects Hindi translation`() {
        val result = parseChangelog(
            raw = """
                {
                  "versionCode": 80,
                  "versionName": "1.2.31",
                  "translations": {
                    "en-US": {"title": "What's new", "notes": ["Added weekly goals"]},
                    "hi-IN": {"title": "नया क्या है", "notes": ["साप्ताहिक लक्ष्य जोड़े गए"]},
                    "id-ID": {"title": "Yang baru", "notes": ["Target mingguan ditambahkan"]}
                  },
                  "detailsUrl": "https://feedback.example.com/updates"
                }
            """.trimIndent(),
            installedVersionCode = 80,
            preferredLanguageTags = listOf("hi-IN"),
        )

        assertThat(result?.versionCode).isEqualTo(80)
        assertThat(result?.versionName).isEqualTo("1.2.31")
        assertThat(result?.title).isEqualTo("नया क्या है")
        assertThat(result?.notes).containsExactly("साप्ताहिक लक्ष्य जोड़े गए")
    }

    @Test
    fun `legacy Indonesian language code selects id translation`() {
        val result = parseChangelog(
            raw = localizedPayload(versionCode = 80),
            installedVersionCode = 80,
            preferredLanguageTags = listOf("in-ID"),
        )

        assertThat(result?.title).isEqualTo("Yang baru")
        assertThat(result?.notes).containsExactly("Perubahan Indonesia")
    }

    @Test
    fun `unsupported locale falls back to English`() {
        val result = parseChangelog(
            raw = localizedPayload(versionCode = 80),
            installedVersionCode = 80,
            preferredLanguageTags = listOf("fr-FR"),
        )

        assertThat(result?.title).isEqualTo("What's new")
        assertThat(result?.notes).containsExactly("English change")
    }

    @Test
    fun `payload for another installed version is rejected`() {
        val result = parseChangelog(
            raw = """{"versionCode":81,"versionName":"1.2.32","title":"What's new","notes":["New"]}""",
            installedVersionCode = 80,
        )

        assertThat(result).isNull()
    }

    @Test
    fun `malformed or empty payload is rejected`() {
        assertThat(parseChangelog("not-json", 80)).isNull()
        assertThat(
            parseChangelog(
                """{"versionCode":80,"versionName":"1.2.31","title":"What's new","notes":[]}""",
                80,
            )
        ).isNull()
    }

    @Test
    fun `unsafe details URL is removed without rejecting notes`() {
        val result = parseChangelog(
            raw = """{"versionCode":80,"versionName":"1.2.31","title":"What's new","notes":["New"],"detailsUrl":"file:///secret"}""",
            installedVersionCode = 80,
        )

        assertThat(result).isNotNull()
        assertThat(result?.detailsUrl).isNull()
    }

    private fun localizedPayload(versionCode: Long): String = """
        {
          "versionCode": $versionCode,
          "versionName": "1.2.31",
          "translations": {
            "en-US": {"title": "What's new", "notes": ["English change"]},
            "hi-IN": {"title": "नया क्या है", "notes": ["हिंदी बदलाव"]},
            "id-ID": {"title": "Yang baru", "notes": ["Perubahan Indonesia"]}
          },
          "detailsUrl": "https://feedback.example.com/updates"
        }
    """.trimIndent()
}
