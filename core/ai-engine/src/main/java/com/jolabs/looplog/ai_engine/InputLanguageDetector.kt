package com.jolabs.looplog.ai_engine

import com.google.mlkit.nl.languageid.LanguageIdentification
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface InputLanguageDetector {
    suspend fun identifyLanguage(text: String): String
}

class MlKitInputLanguageDetector @Inject constructor() : InputLanguageDetector {
    override suspend fun identifyLanguage(text: String): String = suspendCancellableCoroutine { continuation ->
        val identifier = LanguageIdentification.getClient()
        identifier.identifyLanguage(text)
            .addOnSuccessListener { languageCode ->
                identifier.close()
                if (continuation.isActive) continuation.resume(languageCode)
            }
            .addOnFailureListener { error ->
                identifier.close()
                if (continuation.isActive) continuation.resumeWithException(error)
            }

        continuation.invokeOnCancellation { identifier.close() }
    }
}
