package com.jolabs.looplog.ai_engine.helpers

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class SafetyClassificationTest {

    @Test
    fun `parses safe label with whitespace and mixed case`() {
        assertThat(parseSafetyClassification("  Safe\n"))
            .isEqualTo(SafetyClassification.SAFE)
    }

    @Test
    fun `parses unsafe label`() {
        assertThat(parseSafetyClassification("UNSAFE"))
            .isEqualTo(SafetyClassification.UNSAFE)
    }

    @Test
    fun `rejects explanations instead of guessing a label`() {
        assertThat(parseSafetyClassification("SAFE because this is a fitness goal"))
            .isEqualTo(SafetyClassification.MALFORMED)
    }

    @Test
    fun `prompt identifies broad fitness goals as safe`() {
        assertThat(buildSafetyClassificationPrompt())
            .contains("Broad goals such as \"I want to be in better shape\" are SAFE")
    }
}
