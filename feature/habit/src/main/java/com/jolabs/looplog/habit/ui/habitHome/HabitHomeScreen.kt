package com.jolabs.looplog.habit.ui.habitHome

import Resource
import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowHeightSizeClass
import com.jolabs.looplog.design_system.ui.theme.RoundedPolygonShape
import com.jolabs.looplog.habit.ui.components.HabitListItem
import com.jolabs.looplog.habit.R
import com.jolabs.looplog.habit.alarmManager.HabitAlarmManager
import com.jolabs.looplog.habit.ui.components.DateDisplayComponent
import com.jolabs.looplog.habit.ui.components.ExpandableCalendar
import com.jolabs.looplog.habit.ui.components.rememberExpandableCalendarState
import com.jolabs.looplog.habit.utils.WidgetUpdateUtil
import com.jolabs.looplog.model.HabitBasic
import com.jolabs.looplog.model.HabitStatus
import com.jolabs.looplog.ui.UIEvent
import com.jolabs.looplog.util.DateUtils
import com.jolabs.looplog.util.DateUtils.todayEpochDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate


@Composable
fun HabitHomeRoute(
    habitHomeViewModel: HabitHomeViewModel = hiltViewModel(),
    onCreatePress: (habitId: Long) -> Unit
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val habitList = habitHomeViewModel.habitList.collectAsStateWithLifecycle()
    val uiState = habitHomeViewModel.uiState.collectAsStateWithLifecycle()
    val showHabitSections by habitHomeViewModel.showHabitSections.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        habitHomeViewModel.uiEvent.collect { event ->
            when (event) {
                UIEvent.NavigateUp -> null
                UIEvent.NavigateBackTwice -> null // Not used in this screen

                is UIEvent.ShowMessage -> {
                    val text = when (val msg = event.message) {
                        is UiMessage.StringRes -> resources.getString(msg.resId)
                        is UiMessage.Text -> msg.message
                    }
                    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                    WidgetUpdateUtil.requestHabitWidgetsRefresh(context)
                }

                is UIEvent.SetupAlarm -> TODO()
                is UIEvent.RemoveAlarm -> TODO()
            }
        }
    }
    HabitHomeScreen(
        habitList = habitList.value,
        onCreatePress = onCreatePress,
        selectedDate = uiState.value.selectedDate,
        optionList = uiState.value.options,
        selectedOption = uiState.value.selectedOption,
        isCustomDateSelected = uiState.value.isCustomDateSelected,
        optionUpdate = habitHomeViewModel::updateSelectedOption,
        onSelectedDateChange = habitHomeViewModel::updateSelectedDate,
        onSelectedDayChange = habitHomeViewModel::updateSelectedDay,
        updateHabit = habitHomeViewModel::updateHabitEntry,
        updateHabitWidget = { WidgetUpdateUtil.requestHabitWidgetsRefresh(context) },
        getHabitShape = habitHomeViewModel::getHabitShape,
        getFormattedAlarmTime = { habitId, habitTime ->
            habitHomeViewModel.getFormattedAlarmTime(habitId, habitTime, DateFormat.is24HourFormat(context))
        },
        getHabitShapeWrapper = habitHomeViewModel::getHabitShapeWrapper,
        showHabitSections = showHabitSections
    )
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class)
@Composable
internal fun HabitHomeScreen(
    habitList: Resource<List<HabitBasic>> = Resource.Loading(),
    optionList: List<HabitFilterOption>,
    selectedOption: HabitFilterOption?,
    isCustomDateSelected: Boolean,
    optionUpdate: (HabitFilterOption) -> Unit,
    onCreatePress: (habitId: Long) -> Unit,
    updateHabitWidget: () -> Unit,
    selectedDate: Long = todayEpochDay(),
    onSelectedDateChange: (Long) -> Unit = {},
    onSelectedDayChange: (DayOfWeek) -> Unit,
    updateHabit: (Long, Long, HabitStatus) -> Unit = { _, _, _ -> },
    getHabitShape: (Long) -> androidx.graphics.shapes.RoundedPolygon = { MaterialShapes.Circle },
    getFormattedAlarmTime: (Long, Long?) -> String? = { _, _ -> null },
    getHabitShapeWrapper: (Long) -> androidx.compose.ui.graphics.Shape = { RoundedPolygonShape(MaterialShapes.Circle) },
    showHabitSections: Boolean = true
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    val calendarState = rememberExpandableCalendarState()
    var pullOffsetPx by remember { mutableFloatStateOf(0f) }

    // Collapse the calendar when TopBar is fully collapsed
    LaunchedEffect(scrollBehavior) {
        snapshotFlow { scrollBehavior.state.collapsedFraction }
            .collect { collapsedFraction ->
                // Collapse calendar when TopBar is fully collapsed
                val topBarFullyCollapsed = collapsedFraction >= 1.0f

                if (topBarFullyCollapsed && calendarState.isExpanded) {
                    calendarState.collapse()
                }
            }
    }

    // NestedScrollConnection for pull-to-expand calendar
    val pullToExpandConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val atTop = listState.firstVisibleItemIndex == 0 &&
                        listState.firstVisibleItemScrollOffset == 0
                val topBarExpanded = scrollBehavior.state.collapsedFraction == 0f

                // If at top, TopBar expanded, calendar collapsed, and pulling down (available.y > 0)
                if (atTop && topBarExpanded && !calendarState.isExpanded && available.y > 0) {
                    // Accumulate pull offset
                    pullOffsetPx = (pullOffsetPx + available.y).coerceAtLeast(0f)
                    return Offset(0f, available.y) // Consume the scroll
                }

                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                // Check if should expand based on threshold
                val expansionThreshold = 120f
                if (pullOffsetPx > expansionThreshold) {
                    calendarState.expand()
                }
                pullOffsetPx = 0f
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                pullOffsetPx = 0f
                return Velocity.Zero
            }
        }
    }

    // Optimization 1: Memoize the date to prevent the Calendar from
    // recalculating its internal state unless the epoch day actually changes.
    val currentLocalDate = remember(selectedDate) {
        LocalDate.ofEpochDay(selectedDate)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    DateDisplayComponent(
                        selectedDate = selectedDate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->

        // Always show stable Column structure as top-level empty state is handled by NavHost
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Calendar is always here - won't recompose when habitList changes
            ExpandableCalendar(
                selectedDate = currentLocalDate,
                state = calendarState,
                allowExpansion = !currentWindowAdaptiveInfo().windowSizeClass.windowHeightSizeClass.let {
                    it == WindowHeightSizeClass.COMPACT
                },
                externalPullOffset = pullOffsetPx,
                onDateSelected = { date ->
                    if (date.toEpochDay() != selectedDate) {
                        onSelectedDayChange(date.dayOfWeek)
                        onSelectedDateChange(date.toEpochDay())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp)
            )

            Spacer(Modifier.height(8.dp))

            // Only this content area changes based on loading/data state
            Box(modifier = Modifier.weight(1f)) {
                when (val habitListState = habitList) {
                    is Resource.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator(Modifier.size(48.dp))
                        }
                    }

                    is Resource.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyStateView(
                                icon = R.drawable.data_empty,
                                message = stringResource(R.string.habit_load_error)
                            )
                        }
                    }

                    is Resource.Success -> {
                        val habits = habitListState.data ?: emptyList()

                        if (habits.isEmpty()) {
                            // Habits exist in system, but none for this date/filter
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                EmptyStateView(
                                    icon = R.drawable.event_list,
                                    message = stringResource(R.string.no_habits_today)
                                )
                            }
                        } else {
                            // Show habit list
                            val groupedHabits = remember(habits, showHabitSections) {
                                if (showHabitSections) {
                                    habits.groupBy { getHabitTimeSection(it.habitTime) }
                                } else {
                                    mapOf(null to habits)
                                }
                            }

                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .nestedScroll(pullToExpandConnection),
                                contentPadding = PaddingValues(bottom = 100.dp)
                            ) {
                                groupedHabits.forEach { (section, sectionHabits) ->
                                    if (section != null) {
                                        stickyHeader {
                                            HabitSectionHeader(
                                                title = stringResource(section.titleResId),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }

                                    itemsIndexed(
                                        items = sectionHabits,
                                        key = { _, habit -> habit.id }
                                    ) { index, item ->
                                        HabitListItem(
                                            id = item.id,
                                            name = item.name,
                                            description = item.description,
                                            currentStreak = item.currentStreak,
                                            longestStreak = item.longestStreak,
                                            disableComplete = selectedOption == HabitFilterOption.ALL,
                                            alarmTime = getFormattedAlarmTime(item.id, item.habitTime),
                                            habitState = when (item.habitState) {
                                                HabitStatus.NONE -> ToggleableState.Off
                                                HabitStatus.COMPLETED -> ToggleableState.On
                                                HabitStatus.SKIPPED -> ToggleableState.Indeterminate
                                            },
                                            shape = getHabitShapeWrapper(item.id),
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            showRepeatDays = selectedOption == HabitFilterOption.ALL,
                                            repeatDays = item.repeatDays,
                                            onCheckedChange = {
                                                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                                val newStatus = when (item.habitState) {
                                                    HabitStatus.NONE -> HabitStatus.COMPLETED
                                                    HabitStatus.COMPLETED -> HabitStatus.SKIPPED
                                                    HabitStatus.SKIPPED -> HabitStatus.NONE
                                                 }
                                                 updateHabit(item.id, selectedDate, newStatus)
                                             },
                                            onHabitPress = onCreatePress,
                                        )
                                        if (index < sectionHabits.lastIndex) Spacer(Modifier.height(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateView(icon: Int, message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(40.dp)
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(88.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}


@Preview(showBackground = true)
@Composable
internal fun HabitHomeScreenPreview() {
    HabitHomeScreen(
        habitList = Resource.Error(""),
        optionList = HabitFilterOption.entries,
        selectedOption = HabitFilterOption.TODAY,
        isCustomDateSelected = false,
        optionUpdate = {},
        onCreatePress = { },
        onSelectedDateChange = {},
        onSelectedDayChange = {},
        updateHabitWidget = {}
    )
}

private enum class HabitTimeSection(val titleResId: Int, val timeRange: String) {
    LATE_NIGHT(R.string.section_late_night, "12:00 AM – 4:59 AM"),
    MORNING(R.string.section_morning, "5:00 AM – 11:59 AM"),
    AFTERNOON(R.string.section_afternoon, "12:00 PM – 4:59 PM"),
    EVENING(R.string.section_evening, "5:00 PM – 11:59 PM"),
    FLEXIBLE(R.string.section_flexible, "")
}

private fun getHabitTimeSection(timeOfDayMillis: Long?): HabitTimeSection {
    if (timeOfDayMillis == null) return HabitTimeSection.FLEXIBLE

    val localTime = java.time.Instant.ofEpochMilli(timeOfDayMillis)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalTime()

    return when (localTime.hour) {
        in 0..4 -> HabitTimeSection.LATE_NIGHT
        in 5..11 -> HabitTimeSection.MORNING
        in 12..16 -> HabitTimeSection.AFTERNOON
        else -> HabitTimeSection.EVENING
    }
}

@Composable
private fun HabitSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
