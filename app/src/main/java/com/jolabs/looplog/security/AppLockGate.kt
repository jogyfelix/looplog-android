package com.jolabs.looplog.security

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jolabs.looplog.R
import com.jolabs.looplog.model.AppLockMode
import com.jolabs.looplog.settings.R as SettingsR
import com.jolabs.looplog.settings.security.AppLockAuthenticator

/**
 * Full-screen overlay that hides app content until the user authenticates, based on the
 * mode held by [viewModel]. Draw it last (on top) in the activity content so it covers the
 * navigation host and any dialogs while locked.
 */
@Composable
fun AppLockGate(viewModel: AppLockViewModel) {
    val activity = LocalActivity.current as? FragmentActivity ?: return
    val mode by viewModel.lockMode.collectAsStateWithLifecycle()
    val isUnlocked = viewModel.isUnlocked

    val loading = mode == null
    val locked = loading || (mode != AppLockMode.DISABLED && !isUnlocked)
    if (!locked) return

    val title = stringResource(SettingsR.string.app_lock_prompt_title)
    val subtitle = stringResource(SettingsR.string.app_lock_prompt_subtitle)

    // Auto-prompt whenever we (re-)enter the locked state for a known, enabled mode. Keyed on
    // isUnlocked so a re-lock (RESUME) re-prompts, while a user cancel — which leaves isUnlocked
    // untouched — does not loop; the Unlock button covers manual retry.
    LaunchedEffect(mode, isUnlocked) {
        if (!loading && mode != AppLockMode.DISABLED && !isUnlocked) {
            AppLockAuthenticator.authenticate(
                activity = activity,
                title = title,
                subtitle = subtitle,
                onSuccess = viewModel::onAuthenticated,
                onError = {}
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(SettingsR.drawable.fingerprint),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!loading) {
                Text(
                    text = stringResource(R.string.app_lock_locked_message),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Button(
                    onClick = {
                        AppLockAuthenticator.authenticate(
                            activity = activity,
                            title = title,
                            subtitle = subtitle,
                            onSuccess = viewModel::onAuthenticated,
                            onError = {}
                        )
                    },
                    modifier = Modifier.padding(top = 24.dp)
                ) {
                    Text(stringResource(R.string.app_lock_unlock_button))
                }
            }
        }
    }
}
