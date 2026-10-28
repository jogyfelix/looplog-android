package com.jolabs.looplog.navigation

import Resource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.data.repository.HabitRepository
import com.jolabs.looplog.data.repository.ai.AiFeatureAvailability
import com.jolabs.looplog.data.repository.ai.AiFeatureRepository
import com.jolabs.looplog.model.HabitBasic
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AppContentState {
    data object Loading : AppContentState
    data object Empty : AppContentState
    data object Main : AppContentState
    data class Error(val message: String) : AppContentState
}

internal fun AppContentState.reduce(
    resource: Resource<List<HabitBasic>>
): AppContentState = when (resource) {
    is Resource.Loading -> when (this) {
        AppContentState.Empty, AppContentState.Main -> this
        else -> AppContentState.Loading
    }
    is Resource.Success -> {
        if (resource.data.isNullOrEmpty()) AppContentState.Empty else AppContentState.Main
    }
    is Resource.Error -> when (this) {
        AppContentState.Empty, AppContentState.Main -> this
        else -> AppContentState.Error(resource.message ?: "An unexpected error occurred")
    }
}

/**
 * ViewModel for app-level state management.
 * Tracks whether any habits exist in the system to determine
 * whether to show the onboarding empty state or the main tabs interface.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val habitRepository: HabitRepository,
    private val aiFeatureRepository: AiFeatureRepository,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    val aiFeatureAvailability: StateFlow<AiFeatureAvailability> = aiFeatureRepository.availability

    init {
        viewModelScope.launch {
            aiFeatureRepository.refreshAvailability()
        }
    }

    fun trackScreenView(screenName: String) {
        analyticsTracker.trackEvent(AppAnalytics.ScreenViewed(screenName))
    }

    /**
     * Tracks all habits in the system (unfiltered).
     * Used to determine if the user has created any habits at all.
     * When no habits exist, we show a common empty state without the navbar.
     * When habits exist, we show the normal tabbed interface.
     */
    val contentState: StateFlow<AppContentState> = habitRepository.getAllHabits()
        .runningFold(AppContentState.Loading as AppContentState) { state, resource ->
            state.reduce(resource)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppContentState.Loading
        )
}
