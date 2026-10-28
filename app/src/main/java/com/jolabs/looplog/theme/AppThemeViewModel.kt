package com.jolabs.looplog.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jolabs.looplog.data.repository.ThemeRepository
import com.jolabs.looplog.model.ThemePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Supplies the appearance preferences applied around the whole app in MainActivity.
 *
 * The initial value is read synchronously (a single small DataStore read) so the very first
 * frame already uses the saved mode/accent/font scale — this avoids a light↔dark flash on
 * cold start.
 */
@HiltViewModel
class AppThemeViewModel @Inject constructor(
    themeRepository: ThemeRepository
) : ViewModel() {

    private val initial: ThemePreferences = runBlocking { themeRepository.preferences.first() }

    val preferences: StateFlow<ThemePreferences> = themeRepository.preferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, initial)
}
