package com.jolabs.looplog.reminders

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jolabs.looplog.R

/**
 * Warns the user that habit reminders can't fire because the exact-alarm permission or
 * notifications are disabled, with a shortcut to the relevant settings screen.
 */
@Composable
fun ReminderPermissionDialog(
    notificationsBlocked: Boolean,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reminder_permission_title)) },
        text = {
            Text(
                stringResource(
                    if (notificationsBlocked) {
                        R.string.reminder_notification_permission_message
                    } else {
                        R.string.reminder_alarm_permission_message
                    }
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.open_settings)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.later)) }
        }
    )
}