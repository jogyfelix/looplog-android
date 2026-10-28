package com.jolabs.looplog.habit.ui.aiHabitCreate.components

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.jolabs.looplog.habit.R
import com.jolabs.looplog.habit.alarmManager.HabitAlarmManager
import com.jolabs.looplog.habit.ui.aiHabitCreate.AiHabitSuggestion
import com.jolabs.looplog.habit.ui.components.TimePickerDialog
import com.jolabs.looplog.util.DateUtils
import java.util.Calendar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiHabitEditBottomSheet(
    suggestion: AiHabitSuggestion,
    habitAlarmManager: HabitAlarmManager?,
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, timeOfDay: Long?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notificationPermissionMessage = stringResource(R.string.notification_permission_required)
    val enableLabel = stringResource(R.string.enable)
    var title by remember(suggestion.id) { mutableStateOf(suggestion.title) }
    var description by remember(suggestion.id) { mutableStateOf(suggestion.description) }
    var timeOfDay by remember(suggestion.id) { mutableStateOf(suggestion.timeOfDay) }
    var showTimePicker by remember { mutableStateOf(false) }
    val descriptionFocus = remember { FocusRequester() }
    val is24Hour = DateFormat.is24HourFormat(context)
    val timeText = remember(timeOfDay, is24Hour) {
        DateUtils.formatHabitTime(timeOfDay, is24Hour).orEmpty()
    }
    val calendar = Calendar.getInstance()
    val timePickerState = key(timeOfDay) {
        rememberTimePickerState(
            initialHour = timeOfDay?.let {
                Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.HOUR_OF_DAY)
            } ?: calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = timeOfDay?.let {
                Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.MINUTE)
            } ?: calendar.get(Calendar.MINUTE),
            is24Hour = is24Hour,
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showTimePicker = true
        } else {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = notificationPermissionMessage,
                    actionLabel = enableLabel,
                    withDismissAction = true,
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    )
                }
            }
        }
    }
    val openTimePicker = {
        if (habitAlarmManager?.checkNotificationPermission() != false) {
            showTimePicker = true
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = {
                timeOfDay = Calendar.getInstance().apply {
                    set(1970, Calendar.JANUARY, 1, timePickerState.hour, timePickerState.minute, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                showTimePicker = false
            },
        ) {
            TimePicker(state = timePickerState)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
            Text(
                text = stringResource(R.string.habit_edit_habit),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.habit_name)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { descriptionFocus.requestFocus() }),
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(descriptionFocus),
                label = { Text(stringResource(R.string.details)) },
                minLines = 2,
                maxLines = 4,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = timeText,
                    onValueChange = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = openTimePicker),
                    readOnly = true,
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledContainerColor = OutlinedTextFieldDefaults.colors().focusedContainerColor,
                        disabledTextColor = OutlinedTextFieldDefaults.colors().focusedTextColor,
                        disabledLabelColor = OutlinedTextFieldDefaults.colors().unfocusedLabelColor,
                        disabledBorderColor = OutlinedTextFieldDefaults.colors().unfocusedIndicatorColor,
                        disabledTrailingIconColor = OutlinedTextFieldDefaults.colors().focusedTrailingIconColor,
                    ),
                    trailingIcon = if (timeOfDay != null) {
                        {
                            IconButton(onClick = { timeOfDay = null }) {
                                Icon(
                                    painter = painterResource(R.drawable.delete),
                                    contentDescription = stringResource(R.string.remove_time),
                                )
                            }
                        }
                    } else {
                        null
                    },
                    label = { Text(stringResource(R.string.set_time_optional)) },
                )
                Text(
                    text = stringResource(R.string.time_reminder),
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
                Button(
                    onClick = { onSave(title, description, timeOfDay) },
                    enabled = title.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("aiHabitEditSave"),
                ) {
                    Text(stringResource(R.string.save_habit))
                }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
            )
        }
    }
}
