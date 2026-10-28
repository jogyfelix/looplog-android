package com.jolabs.looplog.habit.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.jolabs.looplog.habit.ui.habitHome.HabitFilterOption

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TimeFilterButtonGroup(
    options: List<HabitFilterOption> = HabitFilterOption.entries,
    selectedOption: HabitFilterOption = HabitFilterOption.TODAY,
    onOptionSelected: (HabitFilterOption) -> Unit = {}
) {
    val selectedIndex = options.indexOf(selectedOption)
    Row(
        Modifier,
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, option ->
            ToggleButton(
                checked = selectedIndex == index,
                onCheckedChange = { onOptionSelected(option) },
                modifier = Modifier
                    .semantics { role = Role.RadioButton },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
            ) {

                Text(stringResource(option.resId))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TimeFilterButtonGroupPreview() {
    MaterialTheme {
        var selected by remember { mutableStateOf(HabitFilterOption.TODAY) }

        Column(modifier = Modifier.fillMaxSize()) {
            TimeFilterButtonGroup(
                selectedOption = selected,
                onOptionSelected = { selected = it }
            )
        }
    }
}
