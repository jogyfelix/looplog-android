package com.jolabs.looplog.settings

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.jolabs.looplog.settings.R
import com.jolabs.looplog.settings.backup.BackupEvent
import com.jolabs.looplog.settings.backup.BackupViewModel
import com.jolabs.looplog.habit.widgets.habitList.ToggleHabitWidgetReceiver
import com.jolabs.looplog.model.AppLockMode
import com.jolabs.looplog.settings.security.AppLockAuthenticator
import com.jolabs.looplog.settings.components.SettingsLinkRow
import com.jolabs.looplog.settings.components.SettingsSectionCard
import com.jolabs.looplog.settings.feedback.UserJotFeedbackActivity

/**
 * Settings tab. Wires OS permission state and outbound intents; delegates layout to
 * the stateless [SettingsScreen].
 */
@Composable
fun SettingsRoute(
    onAppearanceClick: () -> Unit = {},
    onWhatsNewClick: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    // Use the application context for imperative getString() calls so we don't capture the
    // Activity context in longer-lived lambdas (toasts, the event collector).
    val appContext = context.applicationContext
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val alarmsAllowed by viewModel.alarmsAllowed.collectAsStateWithLifecycle()
    val appLockMode by viewModel.appLockMode.collectAsStateWithLifecycle()

    var showNotificationSettingsSheet by remember { mutableStateOf(false) }
    var showAppLockDialog by remember { mutableStateOf(false) }
    val appLockLabel = when (appLockMode) {
        AppLockMode.DISABLED -> stringResource(R.string.settings_app_lock_disabled)
        AppLockMode.ON_APP_OPEN_AND_RESUME -> stringResource(R.string.settings_app_lock_enabled)
    }

    val languageOptions = rememberLanguageOptions()
    // Null when following the system language. Compared by language subtag so Indonesian ("in")
    // matches regardless of how AppCompat/the platform normalizes the BCP-47 tag. Keyed on the
    // configuration so it re-reads after a language change updates the locale in place (without
    // which the row/dialog would keep showing the pre-selection value).
    val configuration = LocalConfiguration.current
    val currentLanguageTag = remember(configuration) {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) null else canonicalLanguageCode(locales.get(0)?.language)
    }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val languageLabel = languageOptions.firstOrNull { it.tag == currentLanguageTag }?.label
        ?: stringResource(R.string.language_system_default)

    // --- Backup & restore ---
    val backupViewModel: BackupViewModel = hiltViewModel()
    var showExportSheet by remember { mutableStateOf(false) }
    var showImportPasswordSheet by remember { mutableStateOf(false) }
    var showConfirmRestoreSheet by remember { mutableStateOf(false) }
    var showBackupRestoreSheet by remember { mutableStateOf(false) }
    var showPrivacyTermsSheet by remember { mutableStateOf(false) }
    var pendingExportPassword by remember { mutableStateOf<String?>(null) }

    val exportFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> if (uri != null) backupViewModel.export(uri, pendingExportPassword) }

    val importFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) backupViewModel.onImportFilePicked(uri) }

    LaunchedEffect(Unit) {
        backupViewModel.events.collect { event ->
            fun toast(resId: Int) = Toast.makeText(appContext, appContext.getString(resId), Toast.LENGTH_LONG).show()
            when (event) {
                BackupEvent.ExportSucceeded -> toast(R.string.backup_export_success)
                BackupEvent.ExportFailed -> toast(R.string.backup_export_failed)
                BackupEvent.ImportNeedsPassword -> showImportPasswordSheet = true
                BackupEvent.ImportReadyToConfirm -> {
                    showImportPasswordSheet = false
                    showConfirmRestoreSheet = true
                }
                BackupEvent.ImportNotABackup -> {
                    showConfirmRestoreSheet = false
                    toast(R.string.backup_import_not_a_backup)
                }
                BackupEvent.ImportWrongPassword -> {
                    showConfirmRestoreSheet = false
                    toast(R.string.backup_import_wrong_password)
                    showImportPasswordSheet = true
                }
                is BackupEvent.ImportIncompatible -> {
                    showConfirmRestoreSheet = false
                    toast(R.string.backup_import_incompatible)
                }
                is BackupEvent.ImportSucceeded -> {
                    showConfirmRestoreSheet = false
                    toast(R.string.backup_import_success)
                }
                BackupEvent.ImportFailed -> {
                    showConfirmRestoreSheet = false
                    toast(R.string.backup_import_failed)
                }
            }
        }
    }

    // Re-check on each RESUMED entry so the toggles self-correct when the user returns
    // from the system settings screen after changing a permission.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshPermissionState()
        }
    }

    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }

    SettingsScreen(
        appLockLabel = appLockLabel,
        languageLabel = languageLabel,
        versionName = versionName,
        onItemClick = viewModel::trackItemClick,
        onNotificationSettingsClick = { showNotificationSettingsSheet = true },
        onAppearanceClick = onAppearanceClick,
        onAppLockClick = { showAppLockDialog = true },
        onLanguageClick = { showLanguageDialog = true },
        onBackupRestoreClick = { showBackupRestoreSheet = true },
        onPrivacyTermsClick = { showPrivacyTermsSheet = true },
        onWhatsNewClick = onWhatsNewClick,
        onAddWidgetClick = {
            val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
            val provider = ComponentName(context, ToggleHabitWidgetReceiver::class.java)
            when {
                appWidgetManager == null || !appWidgetManager.isRequestPinAppWidgetSupported -> {
                    Toast.makeText(
                        context,
                        appContext.getString(R.string.settings_add_widget_unsupported),
                        Toast.LENGTH_LONG
                    ).show()
                }

                appWidgetManager.getAppWidgetIds(provider).isNotEmpty() -> {
                    Toast.makeText(
                        context,
                        appContext.getString(R.string.settings_add_widget_already_added),
                        Toast.LENGTH_LONG
                    ).show()
                }

                else -> appWidgetManager.requestPinAppWidget(provider, null, null)
            }
        },
        onSendFeedback = { UserJotFeedbackActivity.open(context) }
    )

    if (showNotificationSettingsSheet) {
        NotificationSettingsSheet(
            notificationsEnabled = notificationsEnabled,
            alarmsAllowed = alarmsAllowed,
            onNotificationsClick = {
                viewModel.trackItemClick(SettingsAnalytics.Item.PUSH_NOTIFICATIONS)
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                )
            },
            onAlarmsClick = {
                viewModel.trackItemClick(SettingsAnalytics.Item.DAILY_REMINDERS)
                context.startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        "package:${context.packageName}".toUri()
                    )
                )
            },
            onDismiss = { showNotificationSettingsSheet = false }
        )
    }

    if (showBackupRestoreSheet) {
        BackupRestoreSheet(
            onExportClick = {
                viewModel.trackItemClick(SettingsAnalytics.Item.EXPORT_DATA)
                showBackupRestoreSheet = false
                showExportSheet = true
            },
            onImportClick = {
                viewModel.trackItemClick(SettingsAnalytics.Item.IMPORT_DATA)
                showBackupRestoreSheet = false
                importFileLauncher.launch(arrayOf("application/octet-stream", "*/*"))
            },
            onDismiss = { showBackupRestoreSheet = false }
        )
    }

    if (showPrivacyTermsSheet) {
        PrivacyTermsSheet(
            onTermsClick = {
                viewModel.trackItemClick(SettingsAnalytics.Item.TERMS_OF_SERVICE)
                showPrivacyTermsSheet = false
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, appContext.getString(R.string.settings_terms_url).toUri())
                )
            },
            onPrivacyClick = {
                viewModel.trackItemClick(SettingsAnalytics.Item.PRIVACY_POLICY)
                showPrivacyTermsSheet = false
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, appContext.getString(R.string.settings_privacy_url).toUri())
                )
            },
            onDismiss = { showPrivacyTermsSheet = false }
        )
    }

    if (showAppLockDialog) {
        AppLockModeDialog(
            selectedMode = appLockMode,
            onSelect = { mode ->
                if (mode == AppLockMode.DISABLED) {
                    viewModel.setAppLockMode(mode)
                    return@AppLockModeDialog
                }
                val activity = context as? FragmentActivity
                if (activity == null || !AppLockAuthenticator.canAuthenticate(context)) {
                    Toast.makeText(
                        context,
                        appContext.getString(R.string.app_lock_unavailable),
                        Toast.LENGTH_LONG
                    ).show()
                    return@AppLockModeDialog
                }
                // Confirm the user can authenticate before committing to an enabled mode,
                // so a lock is never turned on that the user then can't get past.
                AppLockAuthenticator.authenticate(
                    activity = activity,
                    title = appContext.getString(R.string.app_lock_prompt_title),
                    subtitle = appContext.getString(R.string.app_lock_prompt_subtitle),
                    onSuccess = {
                        viewModel.setAppLockMode(mode)
                    },
                    onError = {}
                )
            },
            onDismiss = { showAppLockDialog = false }
        )
    }

    if (showExportSheet) {
        ExportOptionsSheet(
            onExport = { password ->
                pendingExportPassword = password
                showExportSheet = false
                val date = java.time.LocalDate.now()
                exportFileLauncher.launch("${appContext.getString(R.string.backup_file_name)}-$date.llbak")
            },
            onDismiss = { showExportSheet = false }
        )
    }

    if (showImportPasswordSheet) {
        ImportPasswordSheet(
            onSubmit = { password -> backupViewModel.onImportPasswordEntered(password) },
            onDismiss = {
                showImportPasswordSheet = false
                backupViewModel.cancelImport()
            }
        )
    }

    if (showConfirmRestoreSheet) {
        ConfirmRestoreSheet(
            onConfirm = {
                showConfirmRestoreSheet = false
                backupViewModel.confirmImport()
            },
            onDismiss = {
                showConfirmRestoreSheet = false
                backupViewModel.cancelImport()
            }
        )
    }

    if (showLanguageDialog) {
        LanguageDialog(
            options = languageOptions,
            selectedTag = currentLanguageTag,
            onSelect = { tag ->
                showLanguageDialog = false
                if (tag != currentLanguageTag) {
                    // AppCompat/the platform persist this and recreate the activity in the new
                    // language; an empty list means "follow the system language".
                    val locales = if (tag.isNullOrEmpty()) {
                        LocaleListCompat.getEmptyLocaleList()
                    } else {
                        LocaleListCompat.forLanguageTags(tag)
                    }
                    AppCompatDelegate.setApplicationLocales(locales)
                }
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
}

@Composable
internal fun SettingsScreen(
    appLockLabel: String,
    languageLabel: String,
    versionName: String,
    onItemClick: (SettingsAnalytics.Item) -> Unit = {},
    onNotificationSettingsClick: () -> Unit = {},
    onAppearanceClick: () -> Unit = {},
    onAppLockClick: () -> Unit = {},
    onLanguageClick: () -> Unit = {},
    onBackupRestoreClick: () -> Unit = {},
    onPrivacyTermsClick: () -> Unit = {},
    onWhatsNewClick: () -> Unit = {},
    onAddWidgetClick: () -> Unit = {},
    onSendFeedback: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
        )

        // Preferences
        SettingsSectionCard(title = stringResource(R.string.settings_section_preferences)) {
            SettingsLinkRow(
                icon = R.drawable.notifications,
                label = stringResource(R.string.settings_notification_settings),
                onClick = {
                    onItemClick(SettingsAnalytics.Item.NOTIFICATION_SETTINGS)
                    onNotificationSettingsClick()
                }
            )
            SettingsLinkRow(
                icon = R.drawable.theme_mode,
                label = stringResource(R.string.settings_appearance),
                onClick = {
                    onItemClick(SettingsAnalytics.Item.APPEARANCE)
                    onAppearanceClick()
                }
            )
        }

        // Security
        SettingsSectionCard(title = stringResource(R.string.settings_section_security)) {
            SettingsLinkRow(
                icon = R.drawable.fingerprint,
                label = stringResource(R.string.settings_app_lock),
                trailingText = appLockLabel,
                onClick = {
                    onItemClick(SettingsAnalytics.Item.APP_LOCK)
                    onAppLockClick()
                }
            )
        }

        // Language
        SettingsSectionCard(title = stringResource(R.string.settings_section_language)) {
            SettingsLinkRow(
                icon = R.drawable.language,
                label = stringResource(R.string.settings_app_language),
                trailingText = languageLabel,
                onClick = {
                    onItemClick(SettingsAnalytics.Item.APP_LANGUAGE)
                    onLanguageClick()
                }
            )
        }

        // Backup & Restore
        SettingsSectionCard(title = stringResource(R.string.settings_section_backup)) {
            SettingsLinkRow(
                icon = R.drawable.backup,
                label = stringResource(R.string.settings_section_backup),
                onClick = {
                    onItemClick(SettingsAnalytics.Item.BACKUP_RESTORE)
                    onBackupRestoreClick()
                }
            )
        }

        // Widget
        SettingsSectionCard(title = stringResource(R.string.settings_section_widget)) {
            SettingsLinkRow(
                icon = R.drawable.widgets,
                label = stringResource(R.string.settings_add_widget),
                onClick = {
                    onItemClick(SettingsAnalytics.Item.ADD_WIDGET)
                    onAddWidgetClick()
                }
            )
        }

        // Resources
        SettingsSectionCard(title = stringResource(R.string.settings_section_resources)) {
            SettingsLinkRow(
                icon = R.drawable.description,
                label = stringResource(R.string.settings_whats_new),
                onClick = {
                    onItemClick(SettingsAnalytics.Item.WHATS_NEW)
                    onWhatsNewClick()
                }
            )
            SettingsLinkRow(
                icon = R.drawable.privacy_lock,
                label = stringResource(R.string.settings_section_privacy),
                onClick = {
                    onItemClick(SettingsAnalytics.Item.PRIVACY_TERMS)
                    onPrivacyTermsClick()
                }
            )
            SettingsLinkRow(
                icon = R.drawable.feedback_send,
                label = stringResource(R.string.settings_send_feedback),
                onClick = {
                    onItemClick(SettingsAnalytics.Item.SEND_FEEDBACK)
                    onSendFeedback()
                }
            )
        }

        // Version footer
        Text(
            text = stringResource(R.string.settings_version, versionName),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        )
    }
}

/**
 * Normalizes a language subtag to the legacy form used by our resource folders and picker
 * options, so Indonesian matches whether the platform reports it as "id" or "in" (likewise for
 * Hebrew and Yiddish, whose codes also changed).
 */
private fun canonicalLanguageCode(code: String?): String? = when (code?.lowercase()) {
    null, "" -> null
    "id", "in" -> "in"
    "iw", "he" -> "he"
    "ji", "yi" -> "yi"
    else -> code.lowercase()
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    MaterialTheme {
        SettingsScreen(
            appLockLabel = "Disabled",
            languageLabel = "System default",
            versionName = "1.1.23"
        )
    }
}
