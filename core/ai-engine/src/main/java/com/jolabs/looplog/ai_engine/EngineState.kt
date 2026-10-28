package com.jolabs.looplog.ai_engine

sealed class EngineState {
    data object Uninitialized : EngineState()

    // Handles Google Play / AICore download progress
    data class DownloadingModel(val progressPercentage: Int) : EngineState()

    data object WaitingForWifi : EngineState()

    data object LoadingIntoMemory : EngineState()
    data object Ready : EngineState()

    // isRecoverable helps the UI decide whether to show a "Retry Download" button
    data class Error(val message: String, val isRecoverable: Boolean) : EngineState()
}