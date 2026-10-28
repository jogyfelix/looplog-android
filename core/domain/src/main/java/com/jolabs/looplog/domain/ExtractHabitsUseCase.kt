package com.jolabs.looplog.domain

import com.jolabs.looplog.ai_engine.AIEngine
import com.jolabs.looplog.ai_engine.EngineState
import com.jolabs.looplog.ai_engine.InputLanguageDetector
import com.jolabs.looplog.ai_engine.helpers.SafetyClassification
import com.jolabs.looplog.ai_engine.helpers.buildHabitGenerationPrompt
import com.jolabs.looplog.ai_engine.helpers.buildSafetyClassificationInput
import com.jolabs.looplog.ai_engine.helpers.buildSafetyClassificationPrompt
import com.jolabs.looplog.ai_engine.helpers.extractHabitsPayload
import com.jolabs.looplog.ai_engine.helpers.parseSafetyClassification
import com.jolabs.looplog.ai_engine.helpers.scoreHabitsPayload
import com.jolabs.looplog.data.repository.ai.AiFeatureRepository
import com.jolabs.looplog.model.aiDataModels.HabitTemplate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ExtractHabitsUseCase @Inject constructor(
    private val aiEngine: AIEngine,
    private val aiFeatureRepository: AiFeatureRepository,
    private val inputLanguageDetector: InputLanguageDetector,
) {
    suspend operator fun invoke(userInput: String): Result<List<HabitTemplate>> = try {
        if (!aiFeatureRepository.isAiFeatureAvailable()) {
            return Result.failure(Exception("ai_unavailable"))
        }

        val languageCode = try {
            inputLanguageDetector.identifyLanguage(userInput)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            return Result.failure(Exception("language_detection_failed"))
        }
        if (languageCode != "en") {
            return Result.failure(Exception("english_only"))
        }

        if (aiEngine.state.value !is EngineState.Ready) {
            aiEngine.initialize()
        }
        if (aiEngine.state.value !is EngineState.Ready) {
            return Result.failure(Exception("ai_unavailable"))
        }

        try {
            val safetyInstruction = buildSafetyClassificationPrompt()
            aiEngine.startConversation(safetyInstruction, isDeterministic = true)

            val classificationInput = buildSafetyClassificationInput(userInput)
            val rawClassificationResponse = collectResponse(aiEngine.sendMessage(classificationInput))

            val classification = parseSafetyClassification(rawClassificationResponse)
            aiEngine.resetConversation()

            when (classification) {
                SafetyClassification.SAFE -> Unit
                SafetyClassification.UNSAFE -> {
                    return Result.failure(Exception("unsafe"))
                }
                SafetyClassification.MALFORMED -> {
                    return Result.failure(Exception("classification_failed"))
                }
            }

            val generationInstruction = "You are LoopLog's habit generation engine.\n\n" +
                    "Generate exactly 3 safe, practical habits from the user's approved goal.\n\n" +
                    "Each habit must be:\n" +
                    "* small and actionable\n" +
                    "* realistic for an ordinary person\n" +
                    "* directly relevant to the goal\n" +
                    "* meaningfully different from the other two\n\n" +
                    "Write every habit name and description in English.\n" +
                    "Do not provide medical, therapeutic, diagnostic, financial, or professional advice.\n" +
                    "Do not invent facts about the user.\n" +
                    "Do not explain your reasoning.\n" +
                    "Follow the requested output format exactly."

            aiEngine.startConversation(generationInstruction, isDeterministic = true)

            val generationPrompt = buildHabitGenerationPrompt(userInput)
            val rawGenerationResponse = collectResponse(aiEngine.sendMessage(generationPrompt))

            val payload = extractHabitsPayload(rawGenerationResponse)
            val scores = scoreHabitsPayload(payload)

            if (!scores.actionable) {
                return Result.failure(Exception("generation_failed"))
            }

            val templates = payload!!.habits.mapIndexed { index, habit ->
                HabitTemplate(
                    id = "generated_$index",
                    title = habit.name,
                    description = habit.description,
                )
            }
            Result.success(templates)
        } finally {
            aiEngine.resetConversation()
        }
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        Result.failure(Exception("inference_failed"))
    }

    private suspend fun collectResponse(flow: Flow<String>): String {
        val builder = StringBuilder()
        flow.collect { chunk ->
            builder.append(chunk)
        }
        return builder.toString()
    }
}
