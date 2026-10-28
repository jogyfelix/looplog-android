package com.jolabs.looplog.domain

import com.google.common.truth.Truth.assertThat
import com.jolabs.looplog.ai_engine.AIEngine
import com.jolabs.looplog.ai_engine.EngineState
import com.jolabs.looplog.ai_engine.InputLanguageDetector
import com.jolabs.looplog.data.repository.ai.AiFeatureRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Test

class ExtractHabitsUseCaseTest {

    private val aiEngine: AIEngine = mockk(relaxed = true)
    private val aiFeatureRepository: AiFeatureRepository = mockk(relaxed = true)
    private val inputLanguageDetector: InputLanguageDetector = mockk()
    private lateinit var extractHabitsUseCase: ExtractHabitsUseCase
    private val engineStateFlow = MutableStateFlow<EngineState>(EngineState.Ready)

    @BeforeEach
    fun setUp() {
        every { aiEngine.state } returns engineStateFlow
        coEvery { aiFeatureRepository.isAiFeatureAvailable() } returns true
        coEvery { inputLanguageDetector.identifyLanguage(any()) } returns "en"
        extractHabitsUseCase = ExtractHabitsUseCase(aiEngine, aiFeatureRepository, inputLanguageDetector)
    }

    @Test
    fun `unavailable AI does not initialize or run inference`() = runTest {
        coEvery { aiFeatureRepository.isAiFeatureAvailable() } returns false

        val result = extractHabitsUseCase("Build a reading habit")

        assertThat(result.isFailure).isTrue()
        coVerify(exactly = 0) { aiEngine.initialize() }
        coVerify(exactly = 0) { aiEngine.sendMessage(any()) }
    }

    @Test
    fun `Hindi goal is rejected before inference`() = runTest {
        coEvery { inputLanguageDetector.identifyLanguage(any()) } returns "hi"

        val result = extractHabitsUseCase("मैं बेहतर सोना चाहता हूं")

        assertThat(result.exceptionOrNull()?.message).isEqualTo("english_only")
        coVerify(exactly = 0) { aiEngine.initialize() }
        coVerify(exactly = 0) { aiEngine.startConversation(any(), any()) }
        coVerify(exactly = 0) { aiEngine.sendMessage(any()) }
    }

    @Test
    fun `Indonesian goal is rejected before inference`() = runTest {
        coEvery { inputLanguageDetector.identifyLanguage(any()) } returns "id"

        val result = extractHabitsUseCase("Saya ingin tidur lebih baik")

        assertThat(result.exceptionOrNull()?.message).isEqualTo("english_only")
        coVerify(exactly = 0) { aiEngine.startConversation(any(), any()) }
        coVerify(exactly = 0) { aiEngine.sendMessage(any()) }
    }

    @Test
    fun `undetermined goal is rejected before inference`() = runTest {
        coEvery { inputLanguageDetector.identifyLanguage(any()) } returns "und"

        val result = extractHabitsUseCase("gym")

        assertThat(result.exceptionOrNull()?.message).isEqualTo("english_only")
        coVerify(exactly = 0) { aiEngine.startConversation(any(), any()) }
        coVerify(exactly = 0) { aiEngine.sendMessage(any()) }
    }

    @Test
    fun `language detector failure returns general failure before inference`() = runTest {
        coEvery { inputLanguageDetector.identifyLanguage(any()) } throws IllegalStateException("Detector failed")

        val result = extractHabitsUseCase("Build a reading habit")

        assertThat(result.exceptionOrNull()?.message).isEqualTo("language_detection_failed")
        coVerify(exactly = 0) { aiEngine.startConversation(any(), any()) }
        coVerify(exactly = 0) { aiEngine.sendMessage(any()) }
    }

    @Test
    fun `language detector cancellation is propagated`() = runTest {
        coEvery { inputLanguageDetector.identifyLanguage(any()) } throws CancellationException()

        assertThrows<CancellationException> {
            extractHabitsUseCase("Build a reading habit")
        }

        coVerify(exactly = 0) { aiEngine.sendMessage(any()) }
    }

    @Test
    fun `successful safety check and habit generation`() = runTest {
        // Arrange
        val userInput = "I want to be in better shape"
        val safetyResponse = "SAFE"
        val generationResponse = """
            {
              "habits": [
                {
                  "name": "No caffeine after 2pm",
                  "description": "Avoid all caffeinated drinks in the afternoon to improve sleep quality."
                },
                {
                  "name": "Screen-free hour",
                  "description": "Put away all electronic devices one hour before your planned bedtime."
                },
                {
                  "name": "Consistent wake time",
                  "description": "Set an alarm for the same time every morning, including weekends."
                }
              ]
            }
        """.trimIndent()

        coEvery { aiEngine.sendMessage(any()) } returns flowOf(safetyResponse) andThen flowOf(generationResponse)

        // Act
        val result = extractHabitsUseCase(userInput)

        // Assert
        assertThat(result.isSuccess).isTrue()
        val habits = result.getOrNull()
        assertThat(habits).hasSize(3)
        assertThat(habits!![0].title).isEqualTo("No caffeine after 2pm")
        
        coVerify(exactly = 2) { aiEngine.startConversation(any(), isDeterministic = true) }
        coVerify(exactly = 1) {
            aiEngine.startConversation(
                match { it.contains("Write every habit name and description in English") },
                isDeterministic = true,
            )
        }
        coVerify(exactly = 2) { aiEngine.resetConversation() }
    }

    @Test
    fun `unsafe goal should return failure`() = runTest {
        // Arrange
        val userInput = "I want to harm myself"
        val safetyResponse = "UNSAFE"

        coEvery { aiEngine.sendMessage(any()) } returns flowOf(safetyResponse)

        // Act
        val result = extractHabitsUseCase(userInput)

        // Assert
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("unsafe")
        coVerify(exactly = 1) { aiEngine.startConversation(any(), isDeterministic = true) }
        coVerify(exactly = 2) { aiEngine.resetConversation() }
    }

    @Test
    fun `malformed safety response should return classification_failed`() = runTest {
        // Arrange
        coEvery { aiEngine.sendMessage(any()) } returns flowOf("This goal is safe.")

        // Act
        val result = extractHabitsUseCase("I want to be in better shape")

        // Assert
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("classification_failed")
        coVerify(exactly = 1) { aiEngine.startConversation(any(), isDeterministic = true) }
        coVerify(exactly = 2) { aiEngine.resetConversation() }
    }

    @Test
    fun `malformed generation JSON should return generation_failed`() = runTest {
        // Arrange
        val userInput = "I want to sleep better"
        val safetyResponse = "SAFE"
        val generationResponse = "invalid json"

        coEvery { aiEngine.sendMessage(any()) } returns flowOf(safetyResponse) andThen flowOf(generationResponse)

        // Act
        val result = extractHabitsUseCase(userInput)

        // Assert
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("generation_failed")
        coVerify(exactly = 2) { aiEngine.startConversation(any(), isDeterministic = true) }
        coVerify(exactly = 2) { aiEngine.resetConversation() }
    }

    @Test
    fun `generation with only 2 habits should be successful`() = runTest {
        // Arrange
        val userInput = "I want to sleep better"
        val safetyResponse = "SAFE"
        val generationResponse = """
            {
              "habits": [
                {
                  "name": "Habit 1",
                  "description": "Task 1 is long enough"
                },
                {
                  "name": "Habit 2",
                  "description": "Task 2 is also long"
                }
              ]
            }
        """.trimIndent()

        coEvery { aiEngine.sendMessage(any()) } returns flowOf(safetyResponse) andThen flowOf(generationResponse)

        // Act
        val result = extractHabitsUseCase(userInput)

        // Assert
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).hasSize(2)
        coVerify(exactly = 2) { aiEngine.startConversation(any(), isDeterministic = true) }
        coVerify(exactly = 2) { aiEngine.resetConversation() }
    }

    @Test
    fun `inference failure does not expose prompt content in returned error`() = runTest {
        val sensitiveCanary = "private-user-goal-8472"
        coEvery { aiEngine.sendMessage(any()) } returns flow {
            throw IllegalStateException("SDK failed while processing $sensitiveCanary")
        }

        val result = extractHabitsUseCase(sensitiveCanary)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("inference_failed")
        assertThat(result.exceptionOrNull()?.message).doesNotContain(sensitiveCanary)
    }
}
