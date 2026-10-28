package com.jolabs.looplog.changelog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jolabs.looplog.data.repository.ChangelogRepository
import com.jolabs.looplog.model.changelog.Changelog
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ChangelogUiState {
    data object Hidden : ChangelogUiState
    data object Loading : ChangelogUiState
    data object Unavailable : ChangelogUiState
    data class Visible(val changelog: Changelog) : ChangelogUiState
}

@HiltViewModel
class ChangelogViewModel @Inject constructor(
    private val repository: ChangelogRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<ChangelogUiState>(ChangelogUiState.Hidden)
    val state: StateFlow<ChangelogUiState> = _state.asStateFlow()
    private var automaticLoadAttempted = false
    private var markedShownVersionCode: Long? = null
    private var observedLanguageTags: List<String>? = null

    fun onLanguageChanged(languageTags: List<String>) {
        val normalizedTags = languageTags.toList()
        val previousTags = observedLanguageTags
        observedLanguageTags = normalizedTags
        if (previousTags == null || previousTags == normalizedTags || _state.value !is ChangelogUiState.Visible) {
            return
        }

        viewModelScope.launch {
            val changelog = runCatching { repository.getCurrentChangelog() }.getOrNull()
                ?: return@launch
            if (observedLanguageTags == normalizedTags && _state.value is ChangelogUiState.Visible) {
                _state.value = ChangelogUiState.Visible(changelog)
            }
        }
    }

    fun loadAutomatically() {
        if (automaticLoadAttempted) return
        automaticLoadAttempted = true
        load(requireEligibility = true)
    }

    fun openManually() {
        load(requireEligibility = false)
    }

    fun dismiss() {
        _state.value = ChangelogUiState.Hidden
    }

    fun onPresented(changelog: Changelog) {
        if (markedShownVersionCode == changelog.versionCode) return
        markedShownVersionCode = changelog.versionCode
        viewModelScope.launch {
            runCatching { repository.markShown(changelog.versionCode) }
        }
    }

    private fun load(requireEligibility: Boolean) {
        viewModelScope.launch {
            _state.value = ChangelogUiState.Loading
            if (requireEligibility && !runCatching {
                    repository.hasPendingAutomaticChangelog()
                }.getOrDefault(false)
            ) {
                _state.value = ChangelogUiState.Hidden
                return@launch
            }
            val changelog = runCatching { repository.getCurrentChangelog() }.getOrNull()
            if (changelog == null) {
                _state.value = if (requireEligibility) {
                    ChangelogUiState.Hidden
                } else {
                    ChangelogUiState.Unavailable
                }
                return@launch
            }
            val eligible = !requireEligibility || runCatching {
                repository.shouldShowAutomatically(changelog)
            }.getOrDefault(false)
            if (!eligible) {
                _state.value = ChangelogUiState.Hidden
                return@launch
            }
            _state.value = ChangelogUiState.Visible(changelog)
        }
    }
}
