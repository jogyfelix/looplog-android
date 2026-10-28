package com.jolabs.looplog.habit.ui.aiHabitCreate.components

import android.text.format.DateFormat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jolabs.looplog.habit.R
import com.jolabs.looplog.habit.ui.aiHabitCreate.AiHabitSuggestion
import com.jolabs.looplog.util.DateUtils
import java.time.DayOfWeek

@Composable
fun SuggestionCard(
    suggestion: AiHabitSuggestion,
    shape: androidx.compose.ui.graphics.Shape = CircleShape,
    onClick: () -> Unit,
    onDayToggle: (DayOfWeek) -> Unit,
    onEdit: () -> Unit,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (suggestion.selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
        label = "bgColor"
    )
    val contentColor = if (suggestion.selected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface
    val subTextColor = if (suggestion.selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
    else MaterialTheme.colorScheme.onSurfaceVariant
    val is24HourFormat = DateFormat.is24HourFormat(LocalContext.current)
    val reminderTime = suggestion.timeOfDay?.let { timeOfDay ->
        DateUtils.formatHabitTime(timeOfDay, is24HourFormat)
    }

    val borderStrokeWidth by animateDpAsState(
        targetValue = if (suggestion.selected) 1.dp else 0.dp,
        label = "borderWidth"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(backgroundColor)
            .then(
                if (suggestion.selected) Modifier.border(
                    borderStrokeWidth,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    RoundedCornerShape(28.dp)
                ) else Modifier
            )
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (suggestion.selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            color = contentColor,
                            shape = shape
                        )
                )
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (suggestion.selected) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
                    .then(
                        if (!suggestion.selected) Modifier.border(
                            2.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            CircleShape
                        ) else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (suggestion.selected) {
                    Icon(
                        painter = painterResource(id = R.drawable.check),
                        contentDescription = "Selected",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = suggestion.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = contentColor
        )
        Text(
            text = suggestion.description,
            style = MaterialTheme.typography.bodyLarge,
            color = subTextColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        reminderTime?.let { time ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.alarm),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
                val days = listOf(
                    DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
                )

                days.forEachIndexed { index, day ->
                    val isDaySelected = suggestion.days.contains(day)
                    DayCircle(
                        label = dayLabels[index],
                        isSelected = isDaySelected,
                        suggestionSelected = suggestion.selected,
                        onClick = { onDayToggle(day) }
                    )
                }
            }

            IconButton(onClick = onEdit) {
                Icon(
                    painter = painterResource(R.drawable.edit),
                    contentDescription = stringResource(R.string.habit_edit_habit),
                    tint = contentColor,
                )
            }
        }
    }
}

@Composable
fun DayCircle(
    label: String,
    isSelected: Boolean,
    suggestionSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (suggestionSelected && isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        Color.Transparent
    }

    val textColor = if (suggestionSelected && isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .then(
                if (suggestionSelected && isSelected) Modifier.background(containerColor)
                else Modifier.border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null // No ripple for individual days as they are small
            ) { 
                onClick() 
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            fontSize = 10.sp
        )
    }
}
