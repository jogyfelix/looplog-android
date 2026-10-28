package com.jolabs.looplog.habit.ui.habitCreate

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowHeightSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import com.jolabs.looplog.habit.R
import com.jolabs.looplog.habit.alarmManager.HabitAlarmManager
import com.jolabs.looplog.habit.ui.components.TimePickerDialog
import com.jolabs.looplog.habit.ui.components.WeekSelector
import com.jolabs.looplog.ui.UIEvent
import java.time.DayOfWeek
import java.util.Calendar
import androidx.core.net.toUri
import com.jolabs.looplog.habit.utils.WidgetUpdateUtil
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateHabitRoute(
    createHabitViewModel: CreateHabitViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit = {},
    habitId: Long,
    fromDetail: Boolean = false
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val selectedDays by createHabitViewModel.selectedDays.collectAsStateWithLifecycle()
    val habitName by createHabitViewModel.habitName.collectAsStateWithLifecycle()
    val habitDescription by createHabitViewModel.habitDescription.collectAsStateWithLifecycle()
    val timeOfDay by createHabitViewModel.timeOfDay.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val habitAlarmManager = createHabitViewModel.habitAlarmManager
    val calendar = Calendar.getInstance()
    val timePickerState = key(timeOfDay) {
        rememberTimePickerState(
            initialHour = timeOfDay?.let {
                val cal = Calendar.getInstance().apply { timeInMillis = it }
                cal.get(Calendar.HOUR_OF_DAY)
            } ?: calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = timeOfDay?.let {
                val cal = Calendar.getInstance().apply { timeInMillis = it }
                cal.get(Calendar.MINUTE)
            } ?: calendar.get(Calendar.MINUTE),
            is24Hour = DateFormat.is24HourFormat(context),
        )
    }

    LaunchedEffect(Unit) {
        createHabitViewModel.getHabit(habitId)
        createHabitViewModel.setFromDetail(fromDetail)
        createHabitViewModel.uiEvent.collect { event ->
            when (event) {
                UIEvent.NavigateUp -> {
                    onNavigateUp()
                }

                UIEvent.NavigateBackTwice -> {
                    // Pop back stack twice to skip both edit screen and detail screen
                    // This returns user to the original screen (home or stats)
                    onNavigateUp() // Pop edit screen
                    onNavigateUp() // Pop detail screen
                }

                is UIEvent.ShowMessage -> {
                    val text = when (val msg = event.message) {
                        is UiMessage.StringRes -> resources.getString(msg.resId)
                        is UiMessage.Text -> msg.message
                    }
                    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                    WidgetUpdateUtil.requestHabitWidgetsRefresh(context)
                }

                is UIEvent.SetupAlarm -> Unit
                is UIEvent.RemoveAlarm -> Unit
            }
        }
    }

    CreateHabitScreen(
        context = context,
        resources = resources,
        habitId = habitId,
        selectedDays = selectedDays,
        habitName = habitName,
        habitDescription = habitDescription,
        timeOfDay = timeOfDay,
        habitAlarmManager = habitAlarmManager,
        timePickerState = timePickerState,
        onNavigateUp = onNavigateUp,
        onSelectedDayToggle = createHabitViewModel::onSelectedDayToggle,
        toggleAllDays = createHabitViewModel::toggleAllDays,
        onHabitNameChange = createHabitViewModel::onHabitNameChange,
        onHabitDescriptionChange = createHabitViewModel::onHabitDescriptionChange,
        onTimeOfDayChange = createHabitViewModel::onTimeOfDayChange,
        createHabitPress = createHabitViewModel::createHabit,
        deleteHabitPress = createHabitViewModel::deleteHabit
    )
}


@RequiresApi(Build.VERSION_CODES.S)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateHabitScreen(
    context: Context = LocalContext.current,
    resources: Resources = LocalResources.current,
    habitId: Long = 0L,
    selectedDays: List<DayOfWeek> = emptyList(),
    habitName: String = "",
    habitDescription: String = "",
    timeOfDay: Long? = null,
    habitAlarmManager: HabitAlarmManager? = null,
    timePickerState: TimePickerState = rememberTimePickerState(),
    onNavigateUp: () -> Unit = {},
    onSelectedDayToggle: (DayOfWeek) -> Unit = {},
    toggleAllDays: (Boolean) -> Unit = {},
    onHabitNameChange: (String) -> Unit = {},
    onHabitDescriptionChange: (String) -> Unit = {},
    onTimeOfDayChange: (Long?) -> Unit = {},
    createHabitPress: () -> Unit = {},
    deleteHabitPress: () -> Unit = {},
) {

    val adaptiveInfo = currentWindowAdaptiveInfo()
    val isTabletExpanded =
        adaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED &&
                adaptiveInfo.windowSizeClass.windowHeightSizeClass != WindowHeightSizeClass.COMPACT


    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val is24Hour = remember { DateFormat.is24HourFormat(context) }
    val timeText = remember(timeOfDay, is24Hour) {
        com.jolabs.looplog.util.DateUtils.formatHabitTime(timeOfDay, is24Hour) ?: ""
    }
    val descFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()){

        if(!it){
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = resources.getString(R.string.notification_permission_required),
                    actionLabel = resources.getString(R.string.enable),
                    withDismissAction = true,
                    duration = SnackbarDuration.Long
                )
                // Handle the user's action on the Snackbar
                when (result) {
                    SnackbarResult.ActionPerformed -> {
                        // User clicked "Enable," open app settings
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                    SnackbarResult.Dismissed -> {
                        // User dismissed the Snackbar
                    }
                }
            }
        }else {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = "package:com.jolabs.looplog".toUri()
            }
            context.startActivity( intent, null)
        }
    }

    // Function to handle time picker opening with permission checks
    val openTimePicker: () -> Unit = {
        if(habitAlarmManager?.checkNotificationPermission() == true) {
            showTimePicker = true
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }



    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_habit)) },
                navigationIcon = {
                    IconButton(onClick = { onNavigateUp() }) {
                        Icon(
                            painter = painterResource(R.drawable.back),
                            contentDescription = stringResource(
                                R.string.go_back
                            )
                        )
                    }
                },
                actions = {
                    if (habitId != 0L) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                painter = painterResource(R.drawable.delete),
                                contentDescription = stringResource(R.string.delete_habit)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (showTimePicker) {
            TimePickerDialog(
                onDismiss = { showTimePicker = false },
                onConfirm = {
                    val pickedTime = Calendar.getInstance().apply {
                        // Use a fixed epoch date (1970-01-01) to normalize the time component.
                        // This ensures that chronological sorting in SQL works correctly by
                        // comparing only the time part, ignoring the day it was created.
                        set(1970, Calendar.JANUARY, 1, timePickerState.hour, timePickerState.minute, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    onTimeOfDayChange(pickedTime)
                    showTimePicker = false
                }
            ) {
                TimePicker(
                    state = timePickerState,
                )
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(R.string.delete_habit)) },
                text = { Text(stringResource(R.string.delete_habit_desc)) },
                confirmButton = {
                    TextButton(onClick = {
                        deleteHabitPress()
                        showDeleteDialog = false
                    }) { Text(stringResource(R.string.delete)) }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showDeleteDialog = false }) {
                        Text(stringResource(R.string.dismiss))
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    vertical = 16.dp,
                    horizontal = if (isTabletExpanded) 200.dp else 16.dp
                )
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)

        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = {
                        descFocus.requestFocus()
                    }
                ),
                value = habitName,
                singleLine = true,
                onValueChange = {
                    onHabitNameChange(it)
                },
                label = { Text(stringResource(R.string.habit_name)) }
            )



            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(descFocus),
                value = habitDescription,
                onValueChange = {
                    onHabitDescriptionChange(it)
                },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = {
                        focusManager.clearFocus()
                        openTimePicker()
                    }
                ),
                label = { Text(stringResource(R.string.details)) }
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openTimePicker() },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledContainerColor = OutlinedTextFieldDefaults.colors().focusedContainerColor,
                        disabledTextColor = OutlinedTextFieldDefaults.colors().focusedTextColor,
                        disabledLabelColor = OutlinedTextFieldDefaults.colors().unfocusedLabelColor,
                        disabledLeadingIconColor = OutlinedTextFieldDefaults.colors().focusedLeadingIconColor,
                        disabledTrailingIconColor = OutlinedTextFieldDefaults.colors().focusedTrailingIconColor,
                        disabledPlaceholderColor = OutlinedTextFieldDefaults.colors().focusedPlaceholderColor,
                        disabledBorderColor = OutlinedTextFieldDefaults.colors().unfocusedIndicatorColor,
                    ),
                    value = timeText,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    trailingIcon = {
                        Icon(
                            modifier = Modifier.clickable {
                                onTimeOfDayChange(null)
                            },
                            painter = painterResource(R.drawable.delete),
                            contentDescription = stringResource(R.string.remove_time)
                        )
                    },
                    label = { Text(stringResource(R.string.set_time_optional)) }
                )

                Text(
                    stringResource(R.string.time_reminder),
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            WeekSelector(
                selectedDays = selectedDays,
                onDayToggled = { day ->
                    focusManager.clearFocus()
                    onSelectedDayToggle(day)
                },
                toggleAllDays = { isChecked ->
                    focusManager.clearFocus()
                    toggleAllDays(isChecked)
                }
            )
            Spacer(Modifier.height(10.dp))

            Button(
                onClick = createHabitPress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(stringResource(R.string.save_habit))
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
internal fun CreateHabitScreenPreview() {
    CreateHabitScreen()
}
