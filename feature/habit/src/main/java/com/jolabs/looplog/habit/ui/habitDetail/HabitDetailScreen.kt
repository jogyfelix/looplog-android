package com.jolabs.looplog.habit.ui.habitDetail

import UiMessage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LoadingIndicatorDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jolabs.looplog.habit.R
import com.jolabs.looplog.habit.ui.components.RepeatDaysDisplay
import com.jolabs.looplog.habit.ui.habitDetail.components.CalendarHeatmap
import com.jolabs.looplog.habit.ui.habitDetail.components.ConsistencyBarChart
import com.jolabs.looplog.habit.ui.habitDetail.components.StreakCard
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Route composable for Habit Detail screen
 */
@Composable
fun HabitDetailRoute(
    habitId: Long,
    onNavigateUp: () -> Unit,
    onEditPress: (Long) -> Unit,
    viewModel: HabitDetailViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(habitId) {
        viewModel.loadHabitDetail(habitId)
    }

    HabitDetailScreen(
        uiState = uiState.value,
        onNavigateUp = onNavigateUp,
        onEditPress = onEditPress
    )
}

/**
 * Habit Detail screen showing comprehensive statistics for a single habit
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun HabitDetailScreen(
    uiState: HabitDetailUiState,
    onNavigateUp: () -> Unit,
    onEditPress: (Long) -> Unit = {}
) {
    var showInfoType by remember { mutableStateOf<InfoType?>(null) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (uiState) {
                            is HabitDetailUiState.Success -> uiState.habitName
                            else -> stringResource(R.string.habit_details_title)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            painter = painterResource(id = R.drawable.back),
                            contentDescription = stringResource(R.string.habit_navigate_back)
                        )
                    }
                },
                actions = {
                    if (uiState is HabitDetailUiState.Success) {
                        IconButton(onClick = { onEditPress(uiState.habitId) }) {
                            Icon(
                                painter = painterResource(id = R.drawable.edit),
                                contentDescription = stringResource(R.string.habit_edit_habit)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        when (uiState) {
            is HabitDetailUiState.Loading -> {
                LoadingState()
            }

            is HabitDetailUiState.Error -> {
                ErrorState(message = uiState.message)
            }

            is HabitDetailUiState.Success -> {
                SuccessState(
                    uiState = uiState,
                    modifier = Modifier.padding(innerPadding),
                    onHeatmapInfoClick = { showInfoType = InfoType.HEATMAP },
                    onConsistencyInfoClick = { showInfoType = InfoType.CONSISTENCY }
                )
            }
        }

        if (showInfoType != null) {
            ModalBottomSheet(
                onDismissRequest = { showInfoType = null },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                InfoBottomSheetContent(type = showInfoType!!)
            }
        }
    }
}

private enum class InfoType {
    HEATMAP, CONSISTENCY
}

@Composable
private fun InfoBottomSheetContent(type: InfoType) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 48.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = when (type) {
                InfoType.HEATMAP -> stringResource(R.string.habit_activity)
                InfoType.CONSISTENCY -> stringResource(R.string.habit_weekly_consistency)
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = when (type) {
                InfoType.HEATMAP -> stringResource(R.string.habit_heatmap_description)
                InfoType.CONSISTENCY -> stringResource(R.string.habit_consistency_description)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (type == InfoType.HEATMAP) {
            Text(
                text = stringResource(R.string.habit_colors_explanation) +
                        "• ${stringResource(R.string.status_completed)}: ${stringResource(R.string.habit_full_color)}\n" +
                        "• ${stringResource(R.string.status_skipped)}: ${stringResource(R.string.habit_faded_color)}\n" +
                        "• ${stringResource(R.string.status_none)}: ${stringResource(R.string.habit_empty_square)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = stringResource(R.string.habit_consistency_benefit),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator(
            modifier = Modifier.size(48.dp),
            polygons = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons
        )
    }
}

@Composable
private fun ErrorState(message: UiMessage) {
    val messageText = when (message) {
        is UiMessage.StringRes -> stringResource(message.resId)
        is UiMessage.Text -> message.message
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.data_empty),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(88.dp)
            )
            Text(
                text = messageText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SuccessState(
    uiState: HabitDetailUiState.Success,
    modifier: Modifier = Modifier,
    onHeatmapInfoClick: () -> Unit = {},
    onConsistencyInfoClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Repeat Days Display
        if (uiState.repeatDays.isNotEmpty()) {
            RepeatDaysDisplay(
                repeatDays = uiState.repeatDays,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Habit Description
        if (uiState.habitDescription.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {

                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Description", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = uiState.habitDescription.trim(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
        }

        // Streak Cards Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StreakCard(
                value = uiState.currentStreak,
                label = stringResource(R.string.habit_current_streak_label),
                iconRes = R.drawable.flame,
                modifier = Modifier.weight(1f)
            )

            StreakCard(
                value = uiState.bestStreak,
                label = stringResource(R.string.habit_best_streak_label),
                iconRes = R.drawable.bar_chart, // Using bar_chart as trophy placeholder
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar Heatmap
        CalendarHeatmap(
            heatmapData = uiState.heatmapData,
            modifier = Modifier.fillMaxWidth(),
            onInfoClick = onHeatmapInfoClick
        )

        // Consistency Bar Chart
        ConsistencyBarChart(
            consistencyData = uiState.consistencyData,
            modifier = Modifier.fillMaxWidth(),
            animated = true,
            onInfoClick = onConsistencyInfoClick
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Motivational message
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.habit_encouragement_msg),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }

        // Bottom spacing
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun HabitDetailScreenPreview() {
    val today = LocalDate.now()
    val repeatDays = listOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    )

    val sampleHeatmapData = (83 downTo 0).map { daysAgo ->
        val date = today.minusDays(daysAgo.toLong())
        val isScheduled = repeatDays.contains(date.dayOfWeek)
        val state = when {
            !isScheduled -> CompletionState.NOT_SCHEDULED
            daysAgo % 3 == 0 -> CompletionState.SKIPPED
            daysAgo % 7 == 0 -> CompletionState.NONE
            else -> CompletionState.COMPLETED
        }
        HeatmapDay(
            date = date,
            state = state,
            isToday = date == today
        )
    }

    val sampleConsistencyData = DayOfWeek.entries.map { day ->
        val isScheduled = repeatDays.contains(day)
        DayOfWeekStat(
            dayOfWeek = day,
            percentage = if (isScheduled) (60..90).random() else 0,
            completedCount = if (isScheduled) 3 else 0,
            totalCount = if (isScheduled) 4 else 0
        )
    }

    MaterialTheme {
        HabitDetailScreen(
            uiState = HabitDetailUiState.Success(
                habitId = 1L,
                habitName = "Morning Exercise",
                habitDescription = "Start every morning with a 30-minute workout and stretching to boost energy throughout the day.",
                currentStreak = 12,
                bestStreak = 45,
                repeatDays = repeatDays,
                heatmapData = sampleHeatmapData,
                consistencyData = sampleConsistencyData
            ),
            onNavigateUp = {},
            onEditPress = {}
        )
    }
}

