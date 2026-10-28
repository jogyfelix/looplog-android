package com.jolabs.looplog.habit.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jolabs.looplog.design_system.ui.theme.RoundedPolygonShape
import com.jolabs.looplog.habit.R
import com.jolabs.looplog.util.DateUtils
import java.time.DayOfWeek
import java.time.format.TextStyle

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun RepeatDaysDisplay(
    repeatDays: List<DayOfWeek>,
    modifier: Modifier = Modifier,
    animated: Boolean = true
) {
    val locale = LocalConfiguration.current.locales[0]
    val dayOfWeekOrder = remember(locale) { DateUtils.orderedDaysOfWeek(locale) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.habit_repeat_days),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            dayOfWeekOrder.forEachIndexed { index, dayOfWeek ->
                DayShape(
                    label = dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                    isSelected = repeatDays.contains(dayOfWeek),
                    animated = animated,
                    delayMillis = index * 50 // Stagger animation for cascade effect
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DayShape(
    label: String,
    isSelected: Boolean,
    animated: Boolean,
    delayMillis: Int = 0
) {
    // Use different shapes for selected vs unselected
    val shape = remember(isSelected) {
        if (isSelected) {
            RoundedPolygonShape(MaterialShapes.Cookie4Sided) // 4-sided polygon for selected
        } else {
            RoundedPolygonShape(MaterialShapes.Circle) // Circle for unselected
        }
    }

    val targetScale = if (isSelected) 1f else 0.85f

    val scale by animateFloatAsState(
        targetValue = if (animated) targetScale else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "DayScale"
    )

    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .scale(scale)
            .clip(shape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RepeatDaysDisplayPreview() {
    MaterialTheme {
        RepeatDaysDisplay(
            repeatDays = listOf(
                DayOfWeek.MONDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.FRIDAY
            )
        )
    }
}
