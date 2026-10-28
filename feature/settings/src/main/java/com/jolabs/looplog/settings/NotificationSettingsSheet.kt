package com.jolabs.looplog.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jolabs.looplog.settings.components.SettingsToggleRow
import com.jolabs.looplog.habit.R as HabitR

/** Shows the system-managed notification and exact-alarm permission controls. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsSheet(
    notificationsEnabled: Boolean,
    alarmsAllowed: Boolean,
    onNotificationsClick: () -> Unit,
    onAlarmsClick: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, bottom = 48.dp, top = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_section_notifications),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 8.dp)
            )
            SettingsToggleRow(
                icon = R.drawable.notifications,
                label = stringResource(R.string.settings_push_notifications),
                description = stringResource(R.string.settings_push_notifications_desc),
                checked = notificationsEnabled,
                onClick = onNotificationsClick
            )
            SettingsToggleRow(
                icon = HabitR.drawable.alarm,
                label = stringResource(R.string.settings_daily_reminders),
                description = stringResource(R.string.settings_daily_reminders_desc),
                checked = alarmsAllowed,
                onClick = onAlarmsClick
            )
        }
    }
}
