package com.jolabs.looplog.habit.ui.habitDetail.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jolabs.looplog.habit.R
import com.jolabs.looplog.habit.ui.habitDetail.DayOfWeekStat
import com.jolabs.looplog.util.DateUtils
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * Bar chart showing consistency distribution across days of the week
 * Displays completion percentage for each day (ordered by the locale's week convention)
 * based on last 30 days
 */
@Composable
fun ConsistencyBarChart(
    consistencyData: List<DayOfWeekStat>,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
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
                        text = stringResource(R.string.habit_weekly_consistency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = stringResource(R.string.habit_last_30_days),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (onInfoClick != null) {
                    IconButton(onClick = onInfoClick) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.habit_consistency_info_desc),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chart container
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                // Sort by the locale's week convention (Sunday-first for en-US/en-IN,
                // Monday-first for most of Europe) so the chart matches the calendar.
                val locale = LocalConfiguration.current.locales[0]
                val sortedData = remember(consistencyData, locale) {
                    val order = DateUtils.orderedDaysOfWeek(locale)
                    consistencyData.sortedBy { order.indexOf(it.dayOfWeek) }
                }

                sortedData.forEach { stat ->
                    ConsistencyBar(
                        dayOfWeek = stat.dayOfWeek,
                        percentage = stat.percentage,
                        completedCount = stat.completedCount,
                        totalCount = stat.totalCount,
                        modifier = Modifier.weight(1f),
                        animated = animated
                    )
                }
            }
        }
    }
}

/**
 * Individual bar representing one day of the week
 */
@Composable
private fun ConsistencyBar(
    dayOfWeek: DayOfWeek,
    percentage: Int,
    completedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
    animated: Boolean = true
) {
    val isNotScheduled = totalCount == 0
    val count = "$completedCount/$totalCount"
    val animatedHeight = remember { Animatable(0f) }

    LaunchedEffect(percentage, animated) {
        if (animated) {
            animatedHeight.animateTo(
                targetValue = percentage / 100f,
                animationSpec = tween(durationMillis = 600, delayMillis = 50)
            )
        } else {
            animatedHeight.snapTo(percentage / 100f)
        }
    }

    val locale = LocalConfiguration.current.locales[0]
    val fullDayName = dayOfWeek.getDisplayName(TextStyle.FULL, locale)
    val barDescription = if (isNotScheduled) {
        stringResource(R.string.habit_consistency_bar_not_scheduled_desc, fullDayName)
    } else {
        stringResource(
            R.string.habit_consistency_bar_scheduled_desc,
            fullDayName,
            percentage,
            completedCount,
            totalCount
        )
    }

    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = barDescription },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Percentage label above bar (or "N/A" for not scheduled)
        if (isNotScheduled) {
            Text(
                text = "—",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
        } else if (percentage > 0) {
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
        } else {
            Spacer(modifier = Modifier.height(18.dp))
        }

        // Bar container (fixed height area)
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(120.dp)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Background bar (empty state)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isNotScheduled) {
                            MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.3f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                        }
                    )
            )

            // Filled bar (animated height) - only for scheduled days
            if (percentage > 0 && !isNotScheduled) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(animatedHeight.value)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Day label
        Text(
            text = dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
                .take(1), // Just first letter (M, T, W, ...)
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isNotScheduled) FontWeight.Normal else FontWeight.Medium,
            color = if (isNotScheduled) {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textAlign = TextAlign.Center
        )

        // Count label (e.g., "3/4") or empty spacer for not scheduled
        Text(
            text = if (isNotScheduled) "" else count,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
