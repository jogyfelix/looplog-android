package com.jolabs.looplog.ui

import UiMessage
import java.time.DayOfWeek

sealed class UIEvent {
    data class ShowMessage(val message: UiMessage) : UIEvent()

    data class SetupAlarm(val habitId: Long,val habitName:String,val timeOfDay: Long, val daysOfWeek: List<DayOfWeek>) : UIEvent()

    data class RemoveAlarm(val habitId:Long,val daysOfWeek: List<DayOfWeek>) : UIEvent()
    data object NavigateUp : UIEvent()

    /**
     * Navigate back multiple screens in the back stack.
     * Used when deleting a habit from the edit screen to skip both the edit screen
     * and the detail screen, returning to the original screen (home or stats).
     */
    data object NavigateBackTwice : UIEvent()
}