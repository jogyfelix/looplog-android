package com.jolabs.looplog.ai_engine.helpers

import com.jolabs.looplog.model.aiDataModels.GeneratedHabit
import com.jolabs.looplog.model.aiDataModels.GeneratedHabitsPayload
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val placeholderValues = setOf("str", "string", "...", "name", "description")

private val json = Json {
    ignoreUnknownKeys = true
}

data class HabitsPayloadScores(
    val json: Boolean,
    val actionable: Boolean,
)

private fun extractBalancedObject(text: String, start: Int): String? {
    if (start >= text.length || text[start] != '{') return null
    var depth = 0
    var inString = false
    var escape = false
    for (index in start until text.length) {
        val char = text[index]
        if (inString) {
            when {
                escape -> escape = false
                char == '\\' -> escape = true
                char == '"' -> inString = false
            }
            continue
        }
        when (char) {
            '"' -> inString = true
            '{' -> depth++
            '}' -> {
                depth--
                if (depth == 0) return text.substring(start, index + 1)
            }
        }
    }
    return null
}

fun extractHabitsPayload(text: String): GeneratedHabitsPayload? {
    for ((index, char) in text.withIndex()) {
        if (char != '{') continue
        val candidate = extractBalancedObject(text, index) ?: continue
        val element = try {
            json.parseToJsonElement(candidate)
        } catch (_: Exception) {
            continue
        }
        val payloadObject = try {
            element.jsonObject
        } catch (_: Exception) {
            continue
        }
        if (!payloadObject.containsKey("habits")) continue
        val normalized = normalizeHabitsPayload(payloadObject)
        return GeneratedHabitsPayload(habits = normalized)
    }
    return null
}

fun normalizeHabitsPayload(payload: JsonObject): List<GeneratedHabit> {
    val habitsArray = payload["habits"]?.jsonArray ?: return emptyList()
    return habitsArray.mapNotNull { habitElement ->
        try {
            val habit = habitElement.jsonObject
            
            val nameElement = habit["name"]
            if (nameElement == null || nameElement is JsonNull) return@mapNotNull null
            val name = nameElement.jsonPrimitive.content

            val descriptionElement = habit["description"]
            if (descriptionElement == null || descriptionElement is JsonNull) return@mapNotNull null
            val description = descriptionElement.jsonPrimitive.content

            GeneratedHabit(
                name = name,
                description = description,
            )
        } catch (_: Exception) {
            null
        }
    }
}

fun isValidHabit(habit: GeneratedHabit): Boolean {
    val name = habit.name.trim()
    val description = habit.description.trim()

    // Basic length checks
    if (name.length < 3 || description.length < 5) return false

    // Placeholder checks
    if (name.lowercase() in placeholderValues) return false
    if (description.lowercase() in placeholderValues) return false

    // Check for obviously malformed text (like "string", "...", "name here")
    val malformedPatterns = listOf(
        "string",
        "description",
        "name here",
        "...",
        "todo",
        "null",
        "undefined"
    )
    if (malformedPatterns.any { name.lowercase().contains(it) }) return false
    if (malformedPatterns.any { description.lowercase().contains(it) }) return false

    return true
}

fun scoreHabitsPayload(payload: GeneratedHabitsPayload?): HabitsPayloadScores {
    if (payload == null) return HabitsPayloadScores(json = false, actionable = false)

    val habits = payload.habits
    val allActionable = habits.isNotEmpty() && habits.all { isValidHabit(it) }

    return HabitsPayloadScores(
        json = true,
        actionable = allActionable,
    )
}
