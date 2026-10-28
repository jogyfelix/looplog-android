package com.jolabs.looplog.habit.ui.aiHabitCreate

import android.content.Context
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.ui.graphics.Shape
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jolabs.looplog.ai_engine.AIEngine
import com.jolabs.looplog.ai_engine.AIAnalytics
import com.jolabs.looplog.ai_engine.EngineState
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.analytics.model.AnalyticsError
import com.jolabs.looplog.data.repository.HabitRepository
import com.jolabs.looplog.data.repository.ai.AiFeatureRepository
import com.jolabs.looplog.data.repository.ai.AiFeatureAvailability
import com.jolabs.looplog.habit.utils.getHabitShape as getHelperHabitShape
import com.jolabs.looplog.habit.utils.getHabitShapeWrapper as getHelperHabitShapeWrapper
import com.jolabs.looplog.domain.ExtractHabitsUseCase
import com.jolabs.looplog.habit.R
import com.jolabs.looplog.habit.alarmManager.HabitAlarmManager
import com.jolabs.looplog.habit.utils.WidgetUpdateUtil
import com.jolabs.looplog.model.CreateHabit
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class AiHabitCreateViewModel @Inject constructor(
    private val extractHabitsUseCase: ExtractHabitsUseCase,
    private val habitRepository: HabitRepository,
    private val aiFeatureRepository: AiFeatureRepository,
    private val aiEngine: AIEngine,
    private val analyticsTracker: AnalyticsTracker,
    @ApplicationContext private val context: Context,
    val habitAlarmManager: HabitAlarmManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiHabitCreateUiState())
    val uiState: StateFlow<AiHabitCreateUiState> = _uiState.asStateFlow()

    private var setupJob: Job? = null
    private var generationJob: Job? = null
    @Volatile
    private var isForeground = false
    private var downloadStarted = false
    private var lifecycleEpoch = 0L

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    fun getHabitShape(id: String): androidx.graphics.shapes.RoundedPolygon {
        return getHelperHabitShape(id)
    }

    fun getHabitShapeWrapper(id: String): Shape {
        return getHelperHabitShapeWrapper(id)
    }

    init {
        observeAiFeatureAvailability()
        observeAiEngineState()
    }

    private fun observeAiFeatureAvailability() {
        viewModelScope.launch {
            aiFeatureRepository.availability.collect { availability ->
                if (availability == AiFeatureAvailability.Unavailable) {
                    _uiState.update { it.copy(setupState = AiSetupState.Unavailable) }
                }
            }
        }
    }

    private fun checkSetupState() {
        setupJob?.cancel()
        setupJob = viewModelScope.launch {
            if (!aiFeatureRepository.isAiFeatureAvailable()) {
                _uiState.update { it.copy(setupState = AiSetupState.Unavailable) }
                return@launch
            }

            val acknowledged = aiFeatureRepository.isAiNoticeAcknowledged.first()
            if (!acknowledged) {
                _uiState.update { it.copy(setupState = AiSetupState.AiNotice) }
            } else if (aiFeatureRepository.isModelDownloaded() || downloadStarted) {
                // Reattach to an in-progress Play download, or reload an installed model.
                aiFeatureRepository.initializeModel()
            } else {
                _uiState.update { it.copy(setupState = AiSetupState.DownloadSetup) }
            }
        }
    }

    fun onForeground() {
        if (isForeground) return
        isForeground = true
        checkSetupState()
    }

    fun onBackground() {
        if (!isForeground) return
        isForeground = false
        lifecycleEpoch++
        setupJob?.cancel()
        generationJob?.cancel()
        setupJob = null
        generationJob = null
        aiFeatureRepository.release()
        _uiState.update {
            it.copy(
                setupState = AiSetupState.Loading,
                isGenerating = false,
                isError = false,
                isIdle = true,
            )
        }
    }

    private fun observeAiEngineState() {
        viewModelScope.launch {
            aiEngine.state.collect { state ->
                Log.d("AiHabitCreateViewModel", "Engine state change: $state")
                when (state) {
                    is EngineState.DownloadingModel -> {
                        _uiState.update { 
                            it.copy(
                                setupState = AiSetupState.DownloadingProgress,
                                downloadProgress = state.progressPercentage / 100f
                            )
                        }
                    }
                    is EngineState.WaitingForWifi -> {
                        _uiState.update { it.copy(setupState = AiSetupState.WaitingForWifi) }
                    }
                    is EngineState.LoadingIntoMemory -> {
                        _uiState.update { it.copy(setupState = AiSetupState.Loading) }
                    }
                    is EngineState.Ready -> {
                        Log.d("AiHabitCreateViewModel", "Processing Ready state. Current setup: ${uiState.value.setupState}")
                        if (_uiState.value.setupState == AiSetupState.DownloadingProgress ||
                            _uiState.value.setupState == AiSetupState.Loading ||
                            _uiState.value.setupState == AiSetupState.DownloadSetup) {
                            _uiState.update { it.copy(setupState = AiSetupState.Ready) }
                        }
                    }
                    is EngineState.Error -> {
                        _uiState.update { it.copy(setupState = AiSetupState.Error) }
                    }
                    is EngineState.Uninitialized -> {
                        Log.i("AI Engine State", "unin: ")
                    }
                }
            }
        }
    }

    fun acknowledgeAiNotice() {
        viewModelScope.launch(Dispatchers.IO) {
            aiFeatureRepository.acknowledgeAiNotice()
            withContext(Dispatchers.Main.immediate) {
                if (isForeground) checkSetupState()
            }
        }
    }

    fun startDownload() {
        downloadStarted = true
        setupJob?.cancel()
        setupJob = viewModelScope.launch(Dispatchers.IO) {
            aiFeatureRepository.initializeModel()
            if (!aiFeatureRepository.isAiFeatureAvailable()) {
                _uiState.update { it.copy(setupState = AiSetupState.Unavailable) }
            }
        }
    }

    fun triggerWifiConfirmation(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        aiEngine.triggerWifiConfirmation(launcher)
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery, isEnglishOnlyError = false) }
    }

    fun onClearQuery() {
        _uiState.update {
            it.copy(
                query = "",
                isError = false,
                isEnglishOnlyError = false,
                isIdle = true,
                suggestions = emptyList(),
            )
        }
    }

    fun resetError() {
        _uiState.update {
            it.copy(
                query = "",
                isError = false,
                isEnglishOnlyError = false,
                isIdle = true,
                suggestions = emptyList(),
            )
        }
    }

    fun generateSuggestions() {
        val query = claimGeneration() ?: return
        val requestEpoch = lifecycleEpoch

        generationJob = viewModelScope.launch {
            delay(500L.milliseconds)

            extractHabitsUseCase(query)
                .onSuccess { templates ->
                    if (requestEpoch != lifecycleEpoch) return@onSuccess
                    val suggestions = templates.map { template ->
                        AiHabitSuggestion(
                            id = template.id,
                            title = template.title,
                            description = template.description,
                            iconRes = R.drawable.start,
                            selected = false,
                        )
                    }
                    _uiState.update { it.copy(isGenerating = false, suggestions = suggestions, isError = false, isIdle = false) }
                    analyticsTracker.trackEvent(AIAnalytics.GenerationSuccess(suggestions.size))
                }
                .onFailure { error ->
                    if (requestEpoch != lifecycleEpoch) return@onFailure
                    if (error.message == "english_only") {
                        _uiState.update {
                            it.copy(isGenerating = false, isEnglishOnlyError = true, isIdle = true)
                        }
                    } else {
                        _uiState.update {
                            it.copy(isGenerating = false, isError = true, isIdle = false)
                        }
                        analyticsTracker.trackEvent(AIAnalytics.GenerationFailure)
                    }
                }
        }
    }

    private fun claimGeneration(): String? {
        while (true) {
            val state = _uiState.value
            if (state.isGenerating || state.query.isBlank()) return null
            val generatingState = state.copy(
                isGenerating = true,
                isError = false,
                isEnglishOnlyError = false,
                isIdle = false,
            )
            if (_uiState.compareAndSet(state, generatingState)) return state.query
        }
    }

    fun toggleSuggestion(suggestionId: String) {
        _uiState.update { state ->
            state.copy(
                suggestions = state.suggestions.map {
                    if (it.id == suggestionId) it.copy(selected = !it.selected) else it
                }
            )
        }
    }

    fun toggleDay(suggestionId: String, day: DayOfWeek) {
        _uiState.update { state ->
            state.copy(
                suggestions = state.suggestions.map {
                    if (it.id == suggestionId) {
                        val newDays = if (it.days.contains(day)) it.days - day else it.days + day
                        it.copy(days = newDays, selected = true)
                    } else it
                }
            )
        }
    }

    fun updateSuggestion(
        suggestionId: String,
        title: String,
        description: String,
        timeOfDay: Long?,
    ) {
        if (title.isBlank()) return

        _uiState.update { state ->
            state.copy(
                suggestions = state.suggestions.map { suggestion ->
                    if (suggestion.id == suggestionId) {
                        suggestion.copy(
                            title = title.trim(),
                            description = description.trim(),
                            timeOfDay = timeOfDay,
                            selected = true,
                        )
                    } else {
                        suggestion
                    }
                }
            )
        }
    }

    fun addSelectedHabits(onComplete: () -> Unit) {
        val selected = _uiState.value.suggestions.filter { it.selected }
        if (selected.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val now = System.currentTimeMillis()
            try {
                selected.forEach { suggestion ->
                    val newHabit = CreateHabit(
                        id = 0L,
                        name = suggestion.title,
                        description = suggestion.description,
                        daysOfWeek = suggestion.days,
                        timeOfDay = suggestion.timeOfDay,
                        createdAt = now,
                        updatedAt = now
                    )
                    val habitId = habitRepository.createHabit(newHabit)
                    suggestion.timeOfDay?.let { timeOfDay ->
                        habitAlarmManager.scheduleHabitAlarm(
                            habitId = habitId,
                            habitName = suggestion.title,
                            timeOfDayMillis = timeOfDay,
                            dayOfWeek = suggestion.days,
                        )
                    }
                }
                // Refresh widgets after adding new habits
                WidgetUpdateUtil.requestHabitWidgetsRefresh(context)
                analyticsTracker.trackEvent(AIAnalytics.HabitsAdded(selected.size))
                onComplete()
            } catch (_: Exception) {
                Log.e("AiHabitCreateViewModel", "Failed to save AI-generated habits")
                _uiState.update { it.copy(isSaving = false, isError = true) }
                analyticsTracker.trackError(
                    AnalyticsError.Hard(
                        throwable = IllegalStateException("Failed to save AI-generated habits"),
                        source = AIAnalytics.Sources.Orchestrator,
                        message = "Failed to save habits from AI"
                    )
                )
            }
        }
    }

    fun clearAllState() {
        _uiState.update { AiHabitCreateUiState() }
    }

    override fun onCleared() {
        setupJob?.cancel()
        generationJob?.cancel()
        super.onCleared()
        aiFeatureRepository.release()
    }

}
