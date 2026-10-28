package com.jolabs.looplog.settings.feedback

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class UserJotNavigationPolicyTest {

    @Test
    fun `feedback url opens the composer on the looplog workspace`() {
        assertThat(UserJotNavigationPolicy.feedbackUrl.toString())
            .isEqualTo("https://feedback.example.com/?openFeedback")
    }

    @Test
    fun `https pages on the looplog workspace stay in the webview`() {
        assertThat(UserJotNavigationPolicy.shouldOpenInWebView("https://feedback.example.com/roadmap"))
            .isTrue()
    }

    @Test
    fun `lookalike and insecure hosts do not stay in the webview`() {
        assertThat(UserJotNavigationPolicy.shouldOpenInWebView("https://feedback.example.com.evil.test/"))
            .isFalse()
        assertThat(UserJotNavigationPolicy.shouldOpenInWebView("http://feedback.example.com/"))
            .isFalse()
    }

    @Test
    fun `malformed urls do not stay in the webview`() {
        assertThat(UserJotNavigationPolicy.shouldOpenInWebView("not a url"))
            .isFalse()
    }

    @Test
    fun `only external https and mail links may leave the app`() {
        assertThat(UserJotNavigationPolicy.canOpenExternally("https://userjot.com/docs"))
            .isTrue()
        assertThat(UserJotNavigationPolicy.canOpenExternally("mailto:support@example.com"))
            .isTrue()
        assertThat(UserJotNavigationPolicy.canOpenExternally("intent://unsafe"))
            .isFalse()
        assertThat(UserJotNavigationPolicy.canOpenExternally("file:///data/local/tmp/file"))
            .isFalse()
    }
}
