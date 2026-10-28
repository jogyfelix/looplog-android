package com.jolabs.looplog.habit.ui.components


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Shape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.jolabs.looplog.design_system.ui.theme.RoundedPolygonShape
import com.jolabs.looplog.habit.R
import java.time.DayOfWeek

@Composable
internal fun HabitListItem(
    id: Long,
    name: String,
    description: String,
    longestStreak: String,
    currentStreak: String,
    alarmTime: String?,
    habitState: ToggleableState,
    shape: Shape,
    modifier: Modifier,
    disableComplete: Boolean = true,
    showRepeatDays: Boolean = false,
    repeatDays: List<DayOfWeek> = emptyList(),
    onCheckedChange: (ToggleableState) -> Unit = {},
    onHabitPress: (Long) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(color = MaterialTheme.colorScheme.surfaceContainer)
            .then(
                if (disableComplete) {
                    Modifier
                } else {
                    when (habitState) {
                        ToggleableState.On -> Modifier.border(
                            width = 2.dp,
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        ToggleableState.Indeterminate -> Modifier.border(
                            width = 2.dp,
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.outline
                        )
                        ToggleableState.Off -> Modifier
                    }
                }
            )
            .combinedClickable(
                interactionSource = null,
                indication = null,
                role = Role.Button,
                onClick = {
                    if (!disableComplete) {
                        val newState = when (habitState) {
                            ToggleableState.Off -> ToggleableState.On
                            ToggleableState.On -> ToggleableState.Indeterminate
                            ToggleableState.Indeterminate -> ToggleableState.Off
                        }
                        onCheckedChange(newState)
                    } else {
                        onHabitPress(id)
                    }
                },
                onLongClick = { onHabitPress(id) }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Tiny shape + Title
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tiny random shape with circular parent
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = shape
                            )
                    )
                }

                // Habit title - larger text for Material 3 Expressive
                Text(
                    text = name.trim(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Right side: Round action indicator
            if (!disableComplete) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            when (habitState) {
                                ToggleableState.On -> MaterialTheme.colorScheme.surfaceContainerHighest
                                ToggleableState.Indeterminate -> MaterialTheme.colorScheme.surfaceContainerHigh
                                ToggleableState.Off -> MaterialTheme.colorScheme.surfaceContainerLow
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when (habitState) {
                        ToggleableState.On -> {
                            Icon(
                                painter = painterResource(R.drawable.check),
                                contentDescription = stringResource(R.string.status_completed),
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        ToggleableState.Indeterminate -> {
                            Icon(
                                painter = painterResource(R.drawable.remove),
                                contentDescription = stringResource(R.string.status_skipped),
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        ToggleableState.Off -> {
                            // Empty circle for uncompleted state
                            val statusNoneDescription = stringResource(R.string.status_none)
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                                    .semantics {
                                        contentDescription = statusNoneDescription
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview(showBackground = true)
@Composable
private fun Preview() {
    HabitListItem(
        id = 1,
        name = "Hello",
        description = "Hello how are you fgsdhfjghgsfaretdryjtkfu fghjfhdgsfawersghtdr fdhjfhtgserfawrsgetdryjtdh hgsfaewrgsehgrhtjdrseg",
        longestStreak = "25",
        currentStreak = "20",
        alarmTime = "10",
        modifier = Modifier,
        shape = RoundedPolygonShape(MaterialShapes.Triangle),
        habitState = ToggleableState.On,
        showRepeatDays = true,
        repeatDays = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
    )
}