package com.jolabs.looplog.data.repository

interface AppReviewRepository {
    /** Records an app open for [todayEpochDay] and returns the distinct-open-day count. */
    suspend fun registerOpenDay(todayEpochDay: Long): Int
    suspend fun isPromptShown(): Boolean
    suspend fun setPromptShown()

    /**
     * Number of distinct days the app must be opened before the rating nudge appears.
     * Sourced from Remote Config so it can be tuned without a release; defaults to 7.
     */
    suspend fun getEngagementDayThreshold(): Int
}
