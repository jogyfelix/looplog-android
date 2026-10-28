package com.jolabs.looplog.ai_engine

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class AIAnalyticsTest {
    @Test
    fun `generation failure contains no arbitrary error parameters`() {
        assertThat(AIAnalytics.GenerationFailure.params).isEmpty()
    }
}
