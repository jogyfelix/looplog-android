package com.jolabs.looplog.review

import android.app.Activity
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.analytics.model.AnalyticsError
import com.jolabs.looplog.analytics.model.ErrorSource
import com.jolabs.looplog.data.repository.AppReviewRepository
import com.jolabs.looplog.di.ApplicationScope
import com.jolabs.looplog.settings.feedback.UserJotFeedbackActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decides when to surface the one-time "enjoying the app?" rating nudge and handles the
 * user's response.
 *
 * [isEligible] reports only that the user has qualified (opened the app on at least the
 * Remote Config-controlled threshold of distinct days and hasn't been shown the prompt yet) —
 * the actual decision to display is made by the caller, which gates it behind higher-priority
 * dialogs.
 * The one-time "shown" flag is persisted by [markPromptShown] at display time, so a nudge that
 * is deferred (because a higher-priority dialog was showing) still reappears on a later open.
 */
@Singleton
class RatingPromptManager @Inject constructor(
    private val appReviewRepository: AppReviewRepository,
    @ApplicationScope private val scope: CoroutineScope,
    private val analyticsTracker: AnalyticsTracker,
    private val playStoreRatingLauncher: PlayStoreRatingLauncher,
) {
    private val _isEligible = MutableStateFlow(false)
    val isEligible: StateFlow<Boolean> = _isEligible.asStateFlow()

    /**
     * Records today's app open. If the user has now reached the engagement threshold and hasn't
     * been prompted yet, marks the nudge eligible.
     */
    fun onAppOpen() {
        scope.launch {
            val distinctDays = appReviewRepository.registerOpenDay(LocalDate.now().toEpochDay())
            val threshold = appReviewRepository.getEngagementDayThreshold()
            if (distinctDays >= threshold && !appReviewRepository.isPromptShown()) {
                _isEligible.value = true
            }
        }
    }

    /** Commits the once-ever flag; call when the nudge is actually displayed. */
    fun markPromptShown() {
        _isEligible.value = false
        scope.launch { appReviewRepository.setPromptShown() }
        analyticsTracker.trackEvent(ReviewAnalytics.ReviewPromptShown)
    }

    /** User is enjoying the app — open the app's Play Store rating page. */
    fun onEnjoyingYes(activity: Activity) {
        playStoreRatingLauncher.open(activity).onFailure { error ->
            analyticsTracker.trackError(
                AnalyticsError.Soft(
                    source = PLAY_STORE_RATING_ERROR_SOURCE,
                    throwable = error,
                    message = "Unable to open Play Store rating page",
                )
            )
        }
        analyticsTracker.trackEvent(ReviewAnalytics.ReviewPromptResult(accepted = true))
    }

    /** User is not enjoying the app — open the UserJot feedback form. */
    fun onEnjoyingNo(activity: Activity) {
        runCatching {
            activity.startActivity(UserJotFeedbackActivity.createIntent(activity))
        }
        analyticsTracker.trackEvent(ReviewAnalytics.ReviewPromptResult(accepted = false))
    }

    private companion object {
        val PLAY_STORE_RATING_ERROR_SOURCE = ErrorSource("play_store_rating")
    }
}
