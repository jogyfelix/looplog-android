package com.jolabs.looplog.security

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jolabs.looplog.data.repository.AppLockRepository
import com.jolabs.looplog.model.AppLockMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Grace window after backgrounding during which the app won't re-prompt (RESUME mode). */
private const val APP_LOCK_GRACE_MILLIS = 30_000L

/**
 * Holds the app-lock gate state. Being activity-scoped, [isUnlocked] survives configuration
 * changes (no re-prompt on rotation) but resets on process death, so a fresh launch always
 * requires authentication when a lock mode is set.
 */
@HiltViewModel
class AppLockViewModel @Inject constructor(
    appLockRepository: AppLockRepository
) : ViewModel() {

    /** Null while the persisted mode is still loading, so the gate can hide content meanwhile. */
    val lockMode: StateFlow<AppLockMode?> = appLockRepository.lockMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    var isUnlocked by mutableStateOf(false)
        private set

    private var backgroundedAtMillis = 0L

    /**
     * True whenever app content should stay hidden: while the mode is still loading, or when
     * a lock is set and the user hasn't authenticated yet. Callers gate other launch-time UI
     * (update flows, dialogs) on this so nothing surfaces behind the lock overlay.
     */
    fun isLocked(): Boolean =
        lockMode.value?.let { it != AppLockMode.DISABLED && !isUnlocked } ?: true

    fun onAuthenticated() {
        isUnlocked = true
    }

    /** Records when the app left the foreground so [onStarted] can measure the grace window. */
    fun onStopped() {
        backgroundedAtMillis = SystemClock.elapsedRealtime()
    }

    /** Re-locks when returning from a long-enough background, but only in RESUME mode. */
    fun onStarted() {
        if (lockMode.value == AppLockMode.ON_APP_OPEN_AND_RESUME &&
            backgroundedAtMillis != 0L &&
            SystemClock.elapsedRealtime() - backgroundedAtMillis > APP_LOCK_GRACE_MILLIS
        ) {
            isUnlocked = false
        }
    }
}
