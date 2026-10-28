package com.jolabs.looplog.storage.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

private val Context.reviewDataStore by preferencesDataStore(name = "review_prefs")
private val lastOpenDayKey = longPreferencesKey("last_open_epoch_day")
private val distinctOpenDaysKey = intPreferencesKey("distinct_open_days")
private val promptShownKey = booleanPreferencesKey("rating_prompt_shown")

/** Persists app-open engagement (distinct days used) and whether the rating nudge was shown. */
class AppReviewStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    /**
     * Records that the app was opened on [todayEpochDay]. If this is a new calendar day,
     * increments the distinct-open-day count. Returns the resulting count.
     */
    suspend fun registerOpenDay(todayEpochDay: Long): Int {
        val prefs = context.reviewDataStore.edit { prefs ->
            if (prefs[lastOpenDayKey] != todayEpochDay) {
                prefs[distinctOpenDaysKey] = (prefs[distinctOpenDaysKey] ?: 0) + 1
                prefs[lastOpenDayKey] = todayEpochDay
            }
        }
        return prefs[distinctOpenDaysKey] ?: 0
    }

    suspend fun isPromptShown(): Boolean {
        return context.reviewDataStore.data.first()[promptShownKey] ?: false
    }

    suspend fun setPromptShown() {
        context.reviewDataStore.edit { it[promptShownKey] = true }
    }
}
