package com.jolabs.looplog.ai_engine

import com.jolabs.looplog.ai_engine.helpers.buildHabitGenerationPrompt
import com.jolabs.looplog.ai_engine.helpers.extractHabitsPayload
import com.jolabs.looplog.ai_engine.helpers.isValidHabit
import com.jolabs.looplog.ai_engine.helpers.scoreHabitsPayload
import com.jolabs.looplog.model.aiDataModels.GeneratedHabit
import com.jolabs.looplog.model.aiDataModels.GeneratedHabitsPayload
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HabitGenerationSchemaTest {

    @Test
    fun buildHabitGenerationPrompt_prefixesGoalWhenMissing() {
        val prompt = buildHabitGenerationPrompt("Practice guitar")
        assertTrue(prompt.contains("USER GOAL:\n\"Practice guitar\""))
        assertTrue(prompt.contains("exactly 3 habits"))
        assertTrue(prompt.contains("write both values in English"))
    }

    @Test
    fun buildHabitGenerationPrompt_containsGoal() {
        val prompt = buildHabitGenerationPrompt("Practice guitar")
        assertTrue(prompt.contains("\"Practice guitar\""))
    }

    @Test
    fun extractHabitsPayload_parsesValidJsonFromNoisyOutput() {
        val raw = """
            Here are habits:
            {
              "habits": [
                {"name": "Practice chords", "description": "Practice chord changes for 15 minutes"},
                {"name": "Learn a song", "description": "Play through one simple song"},
                {"name": "Tune up", "description": "Tune the guitar before practicing"}
              ]
            }
        """.trimIndent()

        val payload = extractHabitsPayload(raw)
        assertNotNull(payload)
        assertEquals(3, payload!!.habits.size)
        assertEquals("Practice chords", payload.habits[0].name)
    }

    @Test
    fun extractHabitsPayload_stripsUnknownKeys() {
        val raw = """
            {"habits": [
              {"name": "Morning walk", "description": "Walk for 20 minutes", "extra": "ignored"}
            ]}
        """.trimIndent()

        val payload = extractHabitsPayload(raw)
        assertNotNull(payload)
        assertEquals(1, payload!!.habits.size)
        assertEquals("Morning walk", payload.habits[0].name)
    }

    @Test
    fun scoreHabitsPayload_validThreeHabitsIsActionable() {
        val payload = GeneratedHabitsPayload(
            habits = listOf(
                GeneratedHabit("Practice chords", "Practice chord changes for 15 minutes"),
                GeneratedHabit("Learn a song", "Play through one simple song"),
                GeneratedHabit("Tune up", "Tune the guitar before practicing"),
            ),
        )
        val scores = scoreHabitsPayload(payload)
        assertTrue(scores.json)
        assertTrue(scores.actionable)
    }

    @Test
    fun scoreHabitsPayload_wrongCountIsActionable() {
        val payload = GeneratedHabitsPayload(
            habits = listOf(
                GeneratedHabit("Practice chords", "Practice chord changes for 15 minutes"),
            ),
        )
        val scores = scoreHabitsPayload(payload)
        assertTrue(scores.json)
        assertTrue(scores.actionable)
    }

    @Test
    fun isValidHabit_rejectsPlaceholdersAndShortNames() {
        assertFalse(isValidHabit(GeneratedHabit("str", "valid description")))
        assertFalse(isValidHabit(GeneratedHabit("ab", "valid description")))
        assertFalse(isValidHabit(GeneratedHabit("Valid name", "description")))
        assertTrue(isValidHabit(GeneratedHabit("Valid name", "Do the thing")))
    }

    @Test
    fun extractHabitsPayload_returnsNullForInvalidJson() {
        assertNull(extractHabitsPayload("not json at all"))
    }
}
