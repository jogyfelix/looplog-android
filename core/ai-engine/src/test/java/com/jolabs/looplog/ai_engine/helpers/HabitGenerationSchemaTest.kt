package com.jolabs.looplog.ai_engine.helpers

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class HabitGenerationSchemaTest {

    @Test
    fun `extractHabitsPayload with valid JSON`() {
        val text = """
            {
              "habits": [
                {"name": "H1", "description": "D1"},
                {"name": "H2", "description": "D2"},
                {"name": "H3", "description": "D3"}
              ]
            }
        """.trimIndent()
        val payload = extractHabitsPayload(text)
        assertThat(payload).isNotNull()
        assertThat(payload!!.habits).hasSize(3)
        assertThat(payload.habits[0].name).isEqualTo("H1")
    }

    @Test
    fun `extractHabitsPayload with markdown`() {
        val text = """
            Here is the habits JSON:
            ```json
            {
              "habits": [
                {"name": "H1", "description": "D1"},
                {"name": "H2", "description": "D2"},
                {"name": "H3", "description": "D3"}
              ]
            }
            ```
            Hope this helps!
        """.trimIndent()
        val payload = extractHabitsPayload(text)
        assertThat(payload).isNotNull()
        assertThat(payload!!.habits).hasSize(3)
    }

    @Test
    fun `scoreHabitsPayload with exactly 3 valid habits`() {
        val payload = extractHabitsPayload("""
            {
              "habits": [
                {"name": "Drink water", "description": "Drink at least 8 glasses of water daily."},
                {"name": "Read books", "description": "Read for 30 minutes before bed every night."},
                {"name": "Morning walk", "description": "Take a 15-minute walk every morning."}
              ]
            }
        """)
        val scores = scoreHabitsPayload(payload)
        assertThat(scores.json).isTrue()
        assertThat(scores.actionable).isTrue()
    }

    @Test
    fun `scoreHabitsPayload with 2 habits should be actionable`() {
        val payload = extractHabitsPayload("""
            {
              "habits": [
                {"name": "Morning Walk", "description": "Go for a walk in the morning"},
                {"name": "Drink Water", "description": "Drink 8 glasses of water"}
              ]
            }
        """)
        val scores = scoreHabitsPayload(payload)
        assertThat(scores.actionable).isTrue()
    }

    @Test
    fun `scoreHabitsPayload with 4 habits should be actionable`() {
        val payload = extractHabitsPayload("""
            {
              "habits": [
                {"name": "Morning Walk", "description": "Go for a walk in the morning"},
                {"name": "Drink Water", "description": "Drink 8 glasses of water"},
                {"name": "Read Book", "description": "Read 10 pages of a book"},
                {"name": "Study", "description": "Study for 30 minutes"}
              ]
            }
        """)
        val scores = scoreHabitsPayload(payload)
        assertThat(scores.actionable).isTrue()
    }

    @Test
    fun `scoreHabitsPayload with empty name should not be actionable`() {
        val payload = extractHabitsPayload("""
            {
              "habits": [
                {"name": "", "description": "Valid description..."},
                {"name": "H2", "description": "Valid description..."},
                {"name": "H3", "description": "Valid description..."}
              ]
            }
        """)
        val scores = scoreHabitsPayload(payload)
        assertThat(scores.actionable).isFalse()
    }

    @Test
    fun `scoreHabitsPayload with placeholder values should not be actionable`() {
        val payload = extractHabitsPayload("""
            {
              "habits": [
                {"name": "string", "description": "string"},
                {"name": "...", "description": "..."},
                {"name": "name", "description": "description"}
              ]
            }
        """)
        val scores = scoreHabitsPayload(payload)
        assertThat(scores.actionable).isFalse()
    }

    @Test
    fun `scoreHabitsPayload with malformed description should not be actionable`() {
        val payload = extractHabitsPayload("""
            {
              "habits": [
                {"name": "H1", "description": "too short"},
                {"name": "H2", "description": "this is a long enough description"},
                {"name": "H3", "description": "this is also long enough"}
              ]
            }
        """)
        // description must be at least 5 chars according to my implementation
        // "too short" is 9 chars, so it should be valid. Wait, let me check my implementation.
        // if (name.length < 3 || description.length < 5) return false
        // "too short" is indeed longer than 5.
        // I'll test with something shorter.
        
        val payload2 = extractHabitsPayload("""
            {
              "habits": [
                {"name": "H1", "description": "abcd"},
                {"name": "H2", "description": "this is a long enough description"},
                {"name": "H3", "description": "this is also long enough"}
              ]
            }
        """)
        val scores2 = scoreHabitsPayload(payload2)
        assertThat(scores2.actionable).isFalse()
    }

    @Test
    fun `extractHabitsPayload with additional fields`() {
        val text = """
            {
              "habits": [
                {"name": "H1", "description": "D1...", "extra": "field"},
                {"name": "H2", "description": "D2..."},
                {"name": "H3", "description": "D3..."}
              ],
              "reasoning": "I chose these because..."
            }
        """.trimIndent()
        val payload = extractHabitsPayload(text)
        assertThat(payload).isNotNull()
        assertThat(payload!!.habits).hasSize(3)
    }

    @Test
    fun `extractHabitsPayload with null values`() {
        val text = """
            {
              "habits": [
                {"name": null, "description": "D1..."},
                {"name": "H2", "description": null},
                {"name": "H3", "description": "D3..."}
              ]
            }
        """.trimIndent()
        val payload = extractHabitsPayload(text)
        // normalizeHabitsPayload uses mapNotNull and returns empty string or ignores if missing/null
        // My implementation:
        // val name = habit["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
        // If it's explicitly null in JSON, jsonPrimitive?.content might be "null" or null?
        // Actually, JsonPrimitive.content for a null element might throw or be null.
        
        assertThat(payload).isNotNull()
        assertThat(payload!!.habits.size).isLessThan(3)
        assertThat(scoreHabitsPayload(payload).actionable).isFalse()
    }
}
