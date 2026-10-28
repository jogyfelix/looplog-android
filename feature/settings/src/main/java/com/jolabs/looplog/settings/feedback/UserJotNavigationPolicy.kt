package com.jolabs.looplog.settings.feedback

import java.net.URI

internal object UserJotNavigationPolicy {
    private const val TRUSTED_HOST = "feedback.example.com"

    val feedbackUrl: URI = URI("https://$TRUSTED_HOST/?openFeedback")

    fun shouldOpenInWebView(url: String): Boolean {
        val uri = url.toUriOrNull() ?: return false
        return uri.scheme.equals("https", ignoreCase = true) &&
            uri.host.equals(TRUSTED_HOST, ignoreCase = true)
    }

    fun canOpenExternally(url: String): Boolean {
        val scheme = url.toUriOrNull()?.scheme ?: return false
        return scheme.equals("https", ignoreCase = true) ||
            scheme.equals("mailto", ignoreCase = true)
    }

    private fun String.toUriOrNull(): URI? = runCatching { URI(this) }.getOrNull()
}
