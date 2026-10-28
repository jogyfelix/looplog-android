package com.jolabs.looplog.habit.ui.habitDetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.jolabs.looplog.habit.R
import com.jolabs.looplog.habit.ui.habitDetail.CompletionState
import com.jolabs.looplog.habit.ui.habitDetail.HeatmapDay
import java.time.format.TextStyle
import java.util.Locale

/**
 * GitHub-style calendar heatmap showing the last 12 weeks of habit activity
 */
@Composable
fun CalendarHeatmap(
    heatmapData: List<HeatmapDay>,
    modifier: Modifier = Modifier,
    onInfoClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.habit_activity),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.habit_last_12_weeks),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (onInfoClick != null) {
                    IconButton(onClick = onInfoClick) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.habit_activity_heatmap_info_desc),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Organize data into weeks (7 days per week, starting from Sunday)
            val weeks = organizeIntoWeeks(heatmapData)

            // Day labels for GitHub-style layout (only show Mon, Wed, Fri)
            val dayLabels = listOf("", "M", "", "W", "", "F", "")

            val scheduledDays = heatmapData.count { it.state != CompletionState.NOT_SCHEDULED }
            val completedDays = heatmapData.count { it.state == CompletionState.COMPLETED }
            val heatmapSummary = stringResource(
                R.string.habit_activity_heatmap_summary,
                completedDays,
                scheduledDays
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics { contentDescription = heatmapSummary },
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // Each row represents a day of the week
                for (dayIndex in 0..6) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Day label for this row (aligned with the row)
                        Box(
                            modifier = Modifier.width(20.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            if (dayLabels[dayIndex].isNotEmpty()) {
                                Text(
                                    text = dayLabels[dayIndex],
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.End
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Cells for this day across all weeks
                        weeks.forEach { week ->
                            val day = week.getOrNull(dayIndex)
                            HeatmapCell(
                                day = day,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend
            HeatmapLegend()
        }
    }
}

/**
 * Organize heatmap data into weeks
 * Each week is a list of 7 days starting from Sunday
 */
private fun organizeIntoWeeks(data: List<HeatmapDay>): List<List<HeatmapDay?>> {
    if (data.isEmpty()) return emptyList()

    val weeks = mutableListOf<MutableList<HeatmapDay?>>()
    var currentWeek = MutableList<HeatmapDay?>(7) { null }

    data.forEach { day ->
        // Sunday = 7, Monday = 1, ... Saturday = 6 (ISO-8601)
        // Convert to 0-indexed where Sunday = 0
        val dayIndex = if (day.date.dayOfWeek.value == 7) 0 else day.date.dayOfWeek.value
        currentWeek[dayIndex] = day

        // If Saturday (index 6) is filled, start a new week
        if (dayIndex == 6) {
            weeks.add(currentWeek)
            currentWeek = MutableList(7) { null }
        }
    }

    // Add the last incomplete week if it has any data
    if (currentWeek.any { it != null }) {
        weeks.add(currentWeek)
    }

    return weeks
}


/**
 * Individual cell in the heatmap grid
 */
@Composable
private fun HeatmapCell(
    day: HeatmapDay?,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHighest

    val (backgroundColor, borderColor) = when (day?.state) {
        CompletionState.COMPLETED -> primaryColor to primaryColor
        CompletionState.SKIPPED -> primaryColor.copy(alpha = 0.3f) to primaryColor.copy(alpha = 0.3f)
        CompletionState.NONE, CompletionState.NOT_SCHEDULED, null -> surfaceColor.copy(alpha = 0.3f) to surfaceColor
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(3.dp))
            .background(backgroundColor)
            .border(
                width = if (day?.isToday == true) 1.5.dp else 0.5.dp,
                color = if (day?.isToday == true) MaterialTheme.colorScheme.onSurface else borderColor,
                shape = RoundedCornerShape(3.dp)
            )
    )
}

/**
 * Legend explaining the heatmap colors
 */
@Composable
private fun HeatmapLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // None/Not Scheduled
        LegendCell(alpha = 0.0f)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.status_none),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Skipped
        LegendCell(alpha = 0.3f)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.status_skipped),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Completed
        LegendCell(alpha = 1.0f)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.status_completed),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun LegendCell(alpha: Float) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHighest

    val color = if (alpha == 0.0f) surfaceColor.copy(alpha = 0.3f) else primaryColor.copy(alpha = alpha)

    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color)
            .border(
                width = 0.5.dp,
                color = surfaceColor,
                shape = RoundedCornerShape(2.dp)
            )
    )
}
