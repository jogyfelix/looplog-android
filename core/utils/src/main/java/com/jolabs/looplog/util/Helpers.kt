package com.jolabs.looplog.util

import kotlinx.serialization.json.Json


//private val json = Json {
//    ignoreUnknownKeys = true   // model adds extra fields → don't crash
//    isLenient = true           // minor formatting issues → still parse
//    coerceInputValues = true   // wrong type → use default instead of crash
//}

// Assuming 'json' is your configured Json instance and UNKNOWN exists on a fallback interface
inline fun <reified T> parseAndValidate(raw: String, fallback: T): T {
    val jsonString = raw
        .substringAfter("{")
        .substringBeforeLast("}")
        .let { "{$it}" }

    return try {
        Json.decodeFromString<T>(jsonString)
    } catch (e: Exception) {
        fallback
    }
}


//// For ClassificationResult
//val classification = parseAndValidate<ClassificationResult>(rawResponse, ClassificationResult.UNKNOWN)
//
//// For CategoryResult
//val category = parseAndValidate<CategoryResult>(rawResponse, CategoryResult.UNKNOWN)