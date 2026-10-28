package com.jolabs.looplog.data.repository

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.jolabs.looplog.model.changelog.Changelog
import com.jolabs.looplog.storage.datastore.ChangelogStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume

private const val KEY_APP_CHANGELOG = "app_changelog"
private const val USERJOT_UPDATES_URL = "https://feedback.example.com/updates"

class ChangelogRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ChangelogRepository {
    private val changelogStore = ChangelogStore(context)
    private val remoteConfig: FirebaseRemoteConfig by lazy { FirebaseRemoteConfig.getInstance() }
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun hasPendingAutomaticChangelog(): Boolean {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val installedVersionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
        return packageInfo.lastUpdateTime > packageInfo.firstInstallTime &&
            changelogStore.getLastShownVersionCode() != installedVersionCode
    }

    override suspend fun getCurrentChangelog(): Changelog? {
        try {
            suspendCancellableCoroutine { continuation ->
                remoteConfig.fetch(0).addOnCompleteListener { fetchTask ->
                    if (!continuation.isActive) return@addOnCompleteListener
                    if (!fetchTask.isSuccessful) {
                        continuation.resume(Unit)
                        return@addOnCompleteListener
                    }
                    remoteConfig.activate().addOnCompleteListener {
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            // The SDK retains the last activated value, which is still safe to read below.
        }

        return parseChangelog(
            raw = remoteConfig.getString(KEY_APP_CHANGELOG),
            installedVersionCode = installedVersionCode(),
            preferredLanguageTags = preferredLanguageTags(),
            json = json,
        )
    }

    override suspend fun shouldShowAutomatically(changelog: Changelog): Boolean {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return shouldShowChangelog(
            installedVersionCode = PackageInfoCompat.getLongVersionCode(packageInfo),
            remoteVersionCode = changelog.versionCode,
            lastShownVersionCode = changelogStore.getLastShownVersionCode(),
            wasUpdatedInstall = packageInfo.lastUpdateTime > packageInfo.firstInstallTime,
        )
    }

    override suspend fun markShown(versionCode: Long) {
        changelogStore.setLastShownVersionCode(versionCode)
    }

    private fun installedVersionCode(): Long {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return PackageInfoCompat.getLongVersionCode(info)
    }

    private fun preferredLanguageTags(): List<String> {
        val locales = context.resources.configuration.locales
        return (0 until locales.size()).map { index -> locales[index].toLanguageTag() }
    }
}

internal fun shouldShowChangelog(
    installedVersionCode: Long,
    remoteVersionCode: Long,
    lastShownVersionCode: Long?,
    wasUpdatedInstall: Boolean,
): Boolean = installedVersionCode == remoteVersionCode &&
    lastShownVersionCode != installedVersionCode &&
    wasUpdatedInstall

@Serializable
private data class RemoteChangelog(
    val versionCode: Long,
    val versionName: String,
    val title: String? = null,
    val notes: List<String>? = null,
    val translations: Map<String, RemoteTranslation> = emptyMap(),
    val detailsUrl: String? = null,
)

@Serializable
private data class RemoteTranslation(
    val title: String,
    val notes: List<String>,
)

internal fun parseChangelog(
    raw: String,
    installedVersionCode: Long,
    preferredLanguageTags: List<String> = listOf("en-US"),
    json: Json = Json { ignoreUnknownKeys = true },
): Changelog? {
    val remote = runCatching { json.decodeFromString<RemoteChangelog>(raw) }.getOrNull()
        ?: return null
    val translation = selectTranslation(remote.translations, preferredLanguageTags)
    val title = translation?.title ?: remote.title.orEmpty()
    val notes = (translation?.notes ?: remote.notes.orEmpty())
        .map(String::trim)
        .filter(String::isNotEmpty)
    if (
        remote.versionCode != installedVersionCode ||
        remote.versionName.isBlank() ||
        title.isBlank() ||
        notes.isEmpty()
    ) return null

    val safeDetailsUrl = remote.detailsUrl
        ?.takeIf { it == USERJOT_UPDATES_URL || it.startsWith("$USERJOT_UPDATES_URL/") }

    return Changelog(
        versionCode = remote.versionCode,
        versionName = remote.versionName.trim(),
        title = title.trim(),
        notes = notes,
        detailsUrl = safeDetailsUrl,
    )
}

private fun selectTranslation(
    translations: Map<String, RemoteTranslation>,
    preferredLanguageTags: List<String>,
): RemoteTranslation? {
    if (translations.isEmpty()) return null
    val normalized = translations.entries.associate { (tag, translation) ->
        normalizeLanguageTag(tag) to translation
    }
    preferredLanguageTags.forEach { preferredTag ->
        val tag = normalizeLanguageTag(preferredTag)
        normalized[tag]?.let { return it }
        val language = tag.substringBefore('-')
        normalized.entries.firstOrNull { it.key.substringBefore('-') == language }
            ?.value
            ?.let { return it }
    }
    return normalized["en-us"] ?: normalized["en"]
}

private fun normalizeLanguageTag(tag: String): String {
    val normalized = tag.trim().replace('_', '-').lowercase()
    return when {
        normalized == "in" -> "id"
        normalized.startsWith("in-") -> "id-${normalized.substringAfter('-')}"
        else -> normalized
    }
}
