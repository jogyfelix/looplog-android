package com.jolabs.looplog

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.jolabs.looplog.data.repository.HabitRepository
import com.jolabs.looplog.analytics.AnalyticsTracker
import com.jolabs.looplog.changelog.ChangelogBottomSheet
import com.jolabs.looplog.changelog.ChangelogUiState
import com.jolabs.looplog.changelog.ChangelogViewModel
import com.jolabs.looplog.design_system.ui.theme.HabitAppTheme
import com.jolabs.looplog.habit.alarmManager.ALARM_NOTIFICATION_CHANNEL_ID
import com.jolabs.looplog.habit.alarmManager.HabitAlarmManager
import com.jolabs.looplog.habit.workers.DailyAlarmVerificationWorker
import com.jolabs.looplog.model.AppLockMode
import com.jolabs.looplog.model.AppThemeMode
import com.jolabs.looplog.model.update.UpdateDecision
import com.jolabs.looplog.navigation.AppNavigationSetup
import com.jolabs.looplog.reminders.ReminderPermissionDialog
import com.jolabs.looplog.review.RatingPromptManager
import com.jolabs.looplog.review.RatingPromptDialog
import com.jolabs.looplog.security.AppLockGate
import com.jolabs.looplog.security.AppLockViewModel
import com.jolabs.looplog.theme.AppThemeViewModel
import com.jolabs.looplog.update.FlexibleUpdateReadyDialog
import com.jolabs.looplog.update.InAppUpdateManager
import com.jolabs.looplog.update.MandatoryUpdateBlockingScreen
import com.jolabs.looplog.update.UpdateGraceBanner
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val appLockViewModel: AppLockViewModel by viewModels()

    private val appThemeViewModel: AppThemeViewModel by viewModels()

    private val changelogViewModel: ChangelogViewModel by viewModels()

    @Inject
    lateinit var inAppUpdateManager: InAppUpdateManager

    @Inject
    lateinit var habitRepository: HabitRepository

    @Inject
    lateinit var habitAlarmManager: HabitAlarmManager

    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    @Inject
    lateinit var ratingPromptManager: RatingPromptManager

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        inAppUpdateManager.onUpdateFlowResult(result.resultCode == RESULT_OK)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        clearAllNotifications()
        createNotificationChannel()
        verifyAlarmsOnAppOpen()
        ratingPromptManager.onAppOpen()
        setContent {
            val configuration = LocalConfiguration.current
            val changelogLanguageTags = remember(configuration) {
                val locales = configuration.locales
                List(locales.size()) { index -> locales[index].toLanguageTag() }
            }
            LaunchedEffect(changelogLanguageTags) {
                changelogViewModel.onLanguageChanged(changelogLanguageTags)
            }

            val themePrefs by appThemeViewModel.preferences.collectAsStateWithLifecycle()
            val darkTheme = when (themePrefs.mode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            // Keep the system bar icon contrast correct when the user forces a mode.
            LaunchedEffect(darkTheme) { applyEdgeToEdge(darkTheme) }

            // Apply the user's font scale on top of the system's, so all sp text resizes.
            val baseDensity = LocalDensity.current
            val scaledDensity = remember(baseDensity, themePrefs.fontScale) {
                Density(baseDensity.density, baseDensity.fontScale * themePrefs.fontScale)
            }

            CompositionLocalProvider(LocalDensity provides scaledDensity) {
              HabitAppTheme(darkTheme = darkTheme, accent = themePrefs.accent) {
                Surface {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AppNavigationSetup(onShowChangelog = changelogViewModel::openManually)

                        // The app lock takes precedence over every other launch-time surface.
                        // While locked (or still resolving the mode), suppress the update flows
                        // and dialogs below so none of them appear — or mark themselves shown —
                        // behind the lock overlay.
                        val lockMode by appLockViewModel.lockMode.collectAsStateWithLifecycle()
                        val appLocked = lockMode == null ||
                            (lockMode != AppLockMode.DISABLED && !appLockViewModel.isUnlocked)

                        var updatesChecked by rememberSaveable { mutableStateOf(false) }
                        LaunchedEffect(appLocked) {
                            if (!appLocked && !updatesChecked) {
                                updatesChecked = true
                                checkForUpdates(changelogViewModel::loadAutomatically)
                            }
                        }

                        val decision by inAppUpdateManager.decision.collectAsStateWithLifecycle()
                        val readyToInstall by inAppUpdateManager.readyToInstall.collectAsStateWithLifecycle()
                        val immediateFlowFailed by inAppUpdateManager.immediateFlowFailed.collectAsStateWithLifecycle()

                        // rememberSaveable so dismissals survive activity recreation
                        // (process death, config changes not covered by configChanges) —
                        // otherwise every recreation would re-show already-dismissed dialogs.
                        var dismissedGraceDays by rememberSaveable { mutableStateOf<Int?>(null) }
                        var flexibleDialogDismissed by rememberSaveable { mutableStateOf(false) }

                        val graceDecision = decision as? UpdateDecision.MandatoryGrace
                        if (graceDecision != null && graceDecision.daysRemaining != dismissedGraceDays &&
                            !appLocked
                        ) {
                            UpdateGraceBanner(
                                daysRemaining = graceDecision.daysRemaining,
                                onDismiss = { dismissedGraceDays = graceDecision.daysRemaining },
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }

                        if (decision == UpdateDecision.MandatoryNow && immediateFlowFailed &&
                            !appLocked
                        ) {
                            MandatoryUpdateBlockingScreen(
                                onRetry = { inAppUpdateManager.startImmediateUpdate(updateLauncher) }
                            )
                        }

                        // Re-arms whenever Play (re-)confirms DOWNLOADED, e.g. on the next
                        // app open, so a dismissed prompt nags again until installed.
                        LaunchedEffect(readyToInstall) {
                            if (readyToInstall) flexibleDialogDismissed = false
                        }

                        if (readyToInstall && !flexibleDialogDismissed && !appLocked) {
                            FlexibleUpdateReadyDialog(
                                onInstall = { inAppUpdateManager.completeFlexibleUpdate() },
                                onDismiss = { flexibleDialogDismissed = true }
                            )
                        }

                        val updateBlockingScreenVisible =
                            decision == UpdateDecision.MandatoryNow && immediateFlowFailed

                        val changelogState by changelogViewModel.state.collectAsStateWithLifecycle()
                        val changelogVisible = changelogState is ChangelogUiState.Visible

                        // Rating nudge — lowest priority. Only shown in a "happy flow": no update
                        // or reminder dialog is relevant this session. Because the reminder-blocked
                        // flags stay true until the permission is granted, dismissing the reminder
                        // dialog won't surface this one; it defers to a later open. The one-time
                        // "shown" flag is committed only when the dialog actually appears, so a
                        // deferred nudge survives to a future happy-flow open.
                        val ratingEligible by ratingPromptManager.isEligible.collectAsStateWithLifecycle()
                        val higherPriorityDialogActive = appLocked ||
                            updateBlockingScreenVisible ||
                            (readyToInstall && !flexibleDialogDismissed) ||
                            graceDecision != null ||
                            changelogState is ChangelogUiState.Loading ||
                            changelogVisible

                        var ratingDialogVisible by rememberSaveable { mutableStateOf(false) }
                        LaunchedEffect(ratingEligible, higherPriorityDialogActive) {
                            if (ratingEligible && !higherPriorityDialogActive && !ratingDialogVisible) {
                                ratingDialogVisible = true
                                ratingPromptManager.markPromptShown()
                            }
                        }

                        if (ratingDialogVisible && !changelogVisible) {
                            RatingPromptDialog(
                                onYes = {
                                    ratingPromptManager.onEnjoyingYes(this@MainActivity)
                                    ratingDialogVisible = false
                                },
                                onNo = {
                                    ratingPromptManager.onEnjoyingNo(this@MainActivity)
                                    ratingDialogVisible = false
                                },
                                onDismiss = { ratingDialogVisible = false }
                            )
                        }

                        if (changelogVisible && !appLocked && !updateBlockingScreenVisible &&
                            !(readyToInstall && !flexibleDialogDismissed) && graceDecision == null
                        ) {
                            val changelog = (changelogState as ChangelogUiState.Visible).changelog
                            LaunchedEffect(changelog.versionCode) {
                                changelogViewModel.onPresented(changelog)
                            }
                            ChangelogBottomSheet(
                                changelog = changelog,
                                onDismiss = changelogViewModel::dismiss,
                            )
                        }

                        if (changelogState == ChangelogUiState.Unavailable) {
                            LaunchedEffect(changelogState) {
                                changelogViewModel.dismiss()
                                runCatching {
                                    startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            "https://feedback.example.com/updates".toUri(),
                                        )
                                    )
                                }
                            }
                        }

                        // Drawn last so the lock overlay covers the nav host and every dialog
                        // above while the app is locked.
                        AppLockGate(appLockViewModel)
                    }
                }
              }
            }
        }
    }

    private fun applyEdgeToEdge(darkTheme: Boolean) {
        val style = if (darkTheme) {
            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    private fun areReminderNotificationsBlocked(): Boolean {
        val notificationManager = NotificationManagerCompat.from(this)
        if (!notificationManager.areNotificationsEnabled()) return true
        // Notifications can also be blocked per-channel while the app-level toggle stays on.
        val channel = notificationManager.getNotificationChannel(ALARM_NOTIFICATION_CHANNEL_ID)
        return channel?.importance == NotificationManager.IMPORTANCE_NONE
    }

    private fun openReminderSettings(notificationsBlocked: Boolean) {
        val intent = if (notificationsBlocked) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        } else {
            // Only reachable on Android 12+, since checkAlarmPermission() can't
            // report false below that.
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:$packageName".toUri())
        }
        startActivity(intent)
    }

    /**
     * Re-arms habit alarms every time the app is opened. The 3 AM periodic verification can't
     * run while the app is force-stopped (common with aggressive OEM battery managers), but
     * opening the app lifts the force-stop - making this the earliest reliable heal point.
     *
     * The short delay keeps the worker's DB/AlarmManager work out of the cold-start window
     * so it never competes with first render.
     */
    private fun verifyAlarmsOnAppOpen() {
        WorkManager.getInstance(this).enqueueUniqueWork(
            "alarm_verification_app_open",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<DailyAlarmVerificationWorker>()
                .setInitialDelay(10, TimeUnit.SECONDS)
                .build()
        )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            ALARM_NOTIFICATION_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.notification_channel_description)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun clearAllNotifications() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }

    override fun onStart() {
        super.onStart()
        // Re-evaluates the app lock: in RESUME mode, a long-enough background re-locks here.
        appLockViewModel.onStarted()
    }

    override fun onResume() {
        super.onResume()
        // Hold off while the app lock is engaged so no update state work runs behind the lock;
        // the deferred checkForUpdates() picks it up once the user authenticates.
        if (appLockViewModel.isLocked()) return
        // Only re-evaluate/refresh state here, don't auto-(re)launch a flow: starting an
        // update flow pauses/resumes this activity, so calling checkForUpdates() (which
        // auto-launches) would immediately relaunch the Play dialog the instant the user
        // backs out of it. Retrying after a decline is handled by the dedicated UI instead
        // (MandatoryUpdateBlockingScreen's retry button, FlexibleUpdateReadyDialog).
        lifecycleScope.launch {
            inAppUpdateManager.evaluate(updateLauncher)
        }
    }

    override fun onStop() {
        super.onStop()
        // Marks the moment the app left the foreground so the app lock can measure the grace window.
        appLockViewModel.onStopped()
        // Install a downloaded flexible update while the app isn't in the foreground, per
        // Google's recommendation, so users who keep dismissing the prompt aren't nagged forever.
        if (inAppUpdateManager.readyToInstall.value) {
            inAppUpdateManager.completeFlexibleUpdate()
        }
    }

    private fun checkForUpdates(onChecked: () -> Unit = {}) {
        lifecycleScope.launch {
            when (inAppUpdateManager.evaluate(updateLauncher)) {
                UpdateDecision.MandatoryNow -> inAppUpdateManager.startImmediateUpdate(updateLauncher)
                UpdateDecision.Flexible -> inAppUpdateManager.startFlexibleUpdate(updateLauncher)
                is UpdateDecision.MandatoryGrace, UpdateDecision.NoUpdate -> Unit
            }
            onChecked()
        }
    }
}
