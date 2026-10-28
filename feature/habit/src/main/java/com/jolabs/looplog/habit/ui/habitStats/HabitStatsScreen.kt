package com.jolabs.looplog.habit.ui.habitStats

import Resource
import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LoadingIndicatorDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jolabs.looplog.habit.R
import com.jolabs.looplog.habit.ui.components.WeekAtAGlanceCard
import com.jolabs.looplog.habit.ui.habitStats.components.GlobalCompletionCard
import com.jolabs.looplog.habit.ui.habitStats.components.HabitStatsCard
import com.jolabs.looplog.habit.ui.habitStats.components.PerfectDaysCard
import com.jolabs.looplog.habit.utils.WidgetUpdateUtil
import com.jolabs.looplog.model.DailyStat
import com.jolabs.looplog.model.HabitBasic
import com.jolabs.looplog.ui.UIEvent
import com.jolabs.looplog.util.DateUtils
import kotlinx.coroutines.launch

/**
 * Stats screen showing all habits
 */
@Composable
fun HabitStatsRoute(
    habitStatsViewModel: HabitStatsViewModel = hiltViewModel(),
    onCreatePress: (habitId: Long) -> Unit,
    onDetailPress: (habitId: Long) -> Unit = {}
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val habitList = habitStatsViewModel.habitList.collectAsStateWithLifecycle()
    val weeklyStats = habitStatsViewModel.weeklyStats.collectAsStateWithLifecycle()
    val hasWeeklyStatsAnimationPlayed = habitStatsViewModel.hasWeeklyStatsAnimationPlayed.collectAsStateWithLifecycle()
    val globalCompletionPercentage = habitStatsViewModel.globalCompletionPercentage.collectAsStateWithLifecycle()
    val perfectDaysCount = habitStatsViewModel.perfectDaysCount.collectAsStateWithLifecycle()

    // Mark the animation as played the first time this route is composed.
    // Because the ViewModel outlives the composable, this flag stays true on re-entry
    // from the back stack, preventing the morph animation from replaying.
    LaunchedEffect(Unit) {
        habitStatsViewModel.markWeeklyStatsAnimationPlayed()
    }

    LaunchedEffect(Unit) {
        habitStatsViewModel.uiEvent.collect { event ->
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

    HabitStatsScreen(
        habitList = habitList.value,
        weeklyStats = weeklyStats.value,
        isWeeklyStatsAnimated = hasWeeklyStatsAnimationPlayed.value,
        globalCompletionPercentage = globalCompletionPercentage.value,
        perfectDaysCount = perfectDaysCount.value,
        onCreatePress = onCreatePress,
        onDetailPress = onDetailPress,
        updateHabitWidget = { WidgetUpdateUtil.requestHabitWidgetsRefresh(context) }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun HabitStatsScreen(
    habitList: Resource<List<HabitBasic>> = Resource.Loading(),
    weeklyStats: List<DailyStat> = emptyList(),
    isWeeklyStatsAnimated: Boolean = false,
    globalCompletionPercentage: Int = 0,
    perfectDaysCount: Int = 0,
    onCreatePress: (habitId: Long) -> Unit,
    onDetailPress: (habitId: Long) -> Unit = {},
    updateHabitWidget: () -> Unit
) {
    // Scroll Behavior
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val is24Hour = remember(context) { DateFormat.is24HourFormat(context) }

    val hasHabits = habitList is Resource.Success && !habitList.data.isNullOrEmpty()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (hasHabits) {
                TopAppBar(
                    title = {
                        StatsTitle(
                            collapsedFraction = scrollBehavior.state.collapsedFraction,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                        )
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    ) { innerPadding ->
        when (val habitListState = habitList) {
            is Resource.Error<*> -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.data_empty),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(88.dp)
                        )
                        Text(
                            text = stringResource(R.string.habit_load_error),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            is Resource.Loading<*> -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator(
                        modifier = Modifier.size(48.dp),
                        polygons = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons
                    )
                }
            }

            is Resource.Success<*> -> {
                if (!habitListState.data.isNullOrEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        // Header: Week at a glance card
                        if (weeklyStats.isNotEmpty()) {
                            item {
                                WeekAtAGlanceCard(
                                    weeklyStats = weeklyStats,
                                    isAnimated = isWeeklyStatsAnimated,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            // Header: Summary cards row
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min)
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    GlobalCompletionCard(
                                        percentage = globalCompletionPercentage,
                                        modifier = Modifier.weight(1f)
                                    )
                                    PerfectDaysCard(
                                        count = perfectDaysCount,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // Sticky section title for habits list
                            stickyHeader {
                                Text(
                                    text = stringResource(R.string.habit_habits_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(top = 16.dp, bottom = 12.dp)
                                )
                            }
                        }

                        // Habit list items
                        itemsIndexed(habitListState.data!!) { index, item ->
                            HabitStatsCard(
                                name = item.name,
                                description = item.description,
                                currentStreak = item.currentStreak,
                                longestStreak = item.longestStreak,
                                alarmTime = DateUtils.formatHabitTime(item.habitTime, is24Hour),
                                onClick = { onDetailPress(item.id) },
                                modifier = Modifier.padding(bottom = if (index == habitListState.data!!.lastIndex) 100.dp else 0.dp),
                                animated = true
                            )

                            if (index < habitListState.data!!.lastIndex) {
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.event_list),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(88.dp)
                            )
                            Text(
                                text = stringResource(R.string.no_habits_created),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsTitle(
    collapsedFraction: Float,
    modifier: Modifier = Modifier
) {
    // Smooth interpolation based on collapse state
    // collapsedFraction: 0.0 = expanded, 1.0 = collapsed

    // Font sizes
    val allFontSize = lerp(24f, 20f, collapsedFraction).sp
    val habitsFontSize = lerp(48f, 20f, collapsedFraction).sp

    // Layout transition: when collapsed show in Row, when expanded show in Column
//    val showAsRow = collapsedFraction > 0.5f

    Box(modifier = modifier) {
        // Expanded state (Column) - fade out as we collapse
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(1f - collapsedFraction),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = stringResource(R.string.habit_all_caps),
                style = MaterialTheme.typography.titleMedium,
                fontSize = allFontSize,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.habit_habits_caps),
                style = MaterialTheme.typography.titleMedium,
                fontSize = habitsFontSize,
                fontWeight = FontWeight.ExtraLight,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Collapsed state (Row) - fade in as we collapse
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .alpha(collapsedFraction),
//            horizontalArrangement = Arrangement.Start,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Text(
//                text = "ALL ",
//                style = MaterialTheme.typography.titleMedium,
//                fontSize = 20.sp,
//                fontWeight = FontWeight.Normal,
//                color = MaterialTheme.colorScheme.onSurface
//            )
//            Text(
//                text = "HABITS",
//                style = MaterialTheme.typography.titleMedium,
//                fontSize = 20.sp,
//                fontWeight = FontWeight.ExtraLight,
//                color = MaterialTheme.colorScheme.onSurface
//            )
//        }
    }
}

@Preview(showBackground = true)
@Composable
internal fun HabitStatsScreenPreview() {
    HabitStatsScreen(
        habitList = Resource.Loading(),
        onCreatePress = { },
        onDetailPress = { },
        updateHabitWidget = {}
    )
}
