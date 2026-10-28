package com.jolabs.looplog.habit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.jolabs.looplog.habit.R

// Note: not currently wired into any screen (no live call sites), but kept accessible-by-default
// so it's ready to use if/when it replaces the inline toggle logic in HabitListItem.

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ActionIconButtonGroup(
    modifier: Modifier = Modifier,
    checkState: ToggleableState = ToggleableState.Off,
    disableComplete: Boolean,
    onCheckStateChange: (ToggleableState) -> Unit = {},
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tri-state Checkbox Button
        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(IconButtonDefaults.extraSmallContainerSize())
                .clip(ButtonGroupDefaults.connectedLeadingButtonShapes().shape)
                .background(
                    color = if (disableComplete) {
                        MaterialTheme.colorScheme.secondaryFixedDim
                    } else if (checkState != ToggleableState.Off) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .triStateToggleable(
                    state = checkState,
                    enabled = !disableComplete,
                    role = Role.Checkbox,
                    onClick = {
                        val newState = when (checkState) {
                            ToggleableState.Off -> ToggleableState.On
                            ToggleableState.On -> ToggleableState.Indeterminate
                            ToggleableState.Indeterminate -> ToggleableState.Off
                        }
                        onCheckStateChange(newState)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = when (checkState) {
                    ToggleableState.On -> painterResource(R.drawable.check)
                    ToggleableState.Indeterminate ->painterResource(R.drawable.remove)
                    ToggleableState.Off -> painterResource(R.drawable.close)
                },
                contentDescription = when (checkState) {
                    ToggleableState.On -> stringResource(R.string.habit_checked)
                    ToggleableState.Indeterminate -> stringResource(R.string.habit_indeterminate)
                    ToggleableState.Off -> stringResource(R.string.habit_unchecked)
                },
                tint = when (checkState) {
                    ToggleableState.On ->   MaterialTheme.colorScheme.onPrimaryContainer
                    ToggleableState.Indeterminate ->   MaterialTheme.colorScheme.onPrimaryContainer
                    ToggleableState.Off ->  MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.size(16.dp)
            )
        }

        // Edit Button
        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(IconButtonDefaults.extraSmallContainerSize())
                .clip(ButtonGroupDefaults.connectedMiddleButtonShapes().shape)
                .background(color = MaterialTheme.colorScheme.surfaceVariant)
                .clickable(role = Role.Button) { onEditClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.edit),
                contentDescription = stringResource(R.string.habit_edit),
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // Delete Button
        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(IconButtonDefaults.extraSmallContainerSize())
                .clip(ButtonGroupDefaults.connectedTrailingButtonShapes().shape)
                .background(color = MaterialTheme.colorScheme.surfaceVariant)
                .clickable(role = Role.Button) { onDeleteClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.delete),
                contentDescription = stringResource(R.string.delete),
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ActionIconButtonGroupPreview() {
    MaterialTheme {
        var checkState by remember { mutableStateOf(ToggleableState.Off) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Tri-state: $checkState")

            ActionIconButtonGroup(
                checkState = checkState,
                disableComplete = false,
                onCheckStateChange = { checkState = it },
                onEditClick = { /* Handle edit */ },
                onDeleteClick = { /* Handle delete */ }
            )
        }
    }
}
