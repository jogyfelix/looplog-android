package com.jolabs.looplog.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jolabs.looplog.habit.R as HabitR
import com.jolabs.looplog.model.AppAccent
import com.jolabs.looplog.model.AppThemeMode
import com.jolabs.looplog.settings.components.SettingsLinkRow
import com.jolabs.looplog.settings.components.SettingsSectionCard
import com.jolabs.looplog.settings.components.SettingsToggleRow
import kotlin.math.roundToInt

@Composable
fun AppearanceScreenRoute(
    onNavigateUp: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val themePreferences by viewModel.themePreferences.collectAsStateWithLifecycle()
    val showHabitSections by viewModel.showHabitSections.collectAsStateWithLifecycle()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAccentDialog by remember { mutableStateOf(false) }

    val themeModeLabel = when (themePreferences.mode) {
        AppThemeMode.SYSTEM -> stringResource(R.string.theme_mode_system)
        AppThemeMode.LIGHT -> stringResource(R.string.theme_mode_light)
        AppThemeMode.DARK -> stringResource(R.string.theme_mode_dark)
    }
    val accentLabel = when (themePreferences.accent) {
        AppAccent.SYSTEM -> stringResource(R.string.accent_system)
        AppAccent.BLUE -> stringResource(R.string.accent_blue)
        AppAccent.VIOLET -> stringResource(R.string.accent_violet)
        AppAccent.TEAL -> stringResource(R.string.accent_teal)
        AppAccent.AMBER -> stringResource(R.string.accent_amber)
        AppAccent.ROSE -> stringResource(R.string.accent_rose)
    }

    AppearanceScreen(
        themeModeLabel = themeModeLabel,
        accentLabel = accentLabel,
        fontScale = themePreferences.fontScale,
        showHabitSections = showHabitSections,
        onNavigateUp = onNavigateUp,
        onThemeClick = {
            viewModel.trackItemClick(SettingsAnalytics.Item.THEME_MODE)
            showThemeDialog = true
        },
        onAccentClick = {
            viewModel.trackItemClick(SettingsAnalytics.Item.ACCENT_COLOR)
            showAccentDialog = true
        },
        onFontScaleChange = {
            viewModel.trackItemClick(SettingsAnalytics.Item.FONT_SIZE)
            viewModel.setFontScale(it)
        },
        onHabitSectionsToggle = {
            viewModel.trackItemClick(SettingsAnalytics.Item.HABIT_SECTIONS)
            viewModel.setShowHabitSections(it)
        }
    )

    if (showThemeDialog) {
        ThemeModeDialog(
            selectedMode = themePreferences.mode,
            onSelect = viewModel::setThemeMode,
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showAccentDialog) {
        AccentColorDialog(
            selectedAccent = themePreferences.accent,
            onSelect = viewModel::setAccent,
            onDismiss = { showAccentDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppearanceScreen(
    themeModeLabel: String,
    accentLabel: String,
    fontScale: Float,
    showHabitSections: Boolean,
    onNavigateUp: () -> Unit,
    onThemeClick: () -> Unit = {},
    onAccentClick: () -> Unit = {},
    onFontScaleChange: (Float) -> Unit = {},
    onHabitSectionsToggle: (Boolean) -> Unit = {}
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_appearance)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.appearance_navigate_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SettingsSectionCard {
                SettingsLinkRow(
                    icon = R.drawable.theme_mode,
                    label = stringResource(R.string.settings_theme_mode),
                    trailingText = themeModeLabel,
                    onClick = onThemeClick
                )
                SettingsLinkRow(
                    icon = R.drawable.palette,
                    label = stringResource(R.string.settings_accent_color),
                    trailingText = accentLabel,
                    onClick = onAccentClick
                )
                FontScaleRow(
                    fontScale = fontScale,
                    onFontScaleChange = onFontScaleChange
                )
                SettingsToggleRow(
                    icon = HabitR.drawable.event_list,
                    label = stringResource(R.string.settings_show_habit_sections),
                    checked = showHabitSections,
                    onClick = { onHabitSectionsToggle(!showHabitSections) }
                )
            }
        }
    }
}

private val FONT_SCALE_STEPS = listOf(0.85f, 1.0f, 1.15f, 1.3f, 1.5f)

private fun nearestFontScaleIndex(value: Float): Int {
    var best = 0
    var bestDiff = Float.MAX_VALUE
    FONT_SCALE_STEPS.forEachIndexed { index, step ->
        val diff = kotlin.math.abs(step - value)
        if (diff < bestDiff) {
            bestDiff = diff
            best = index
        }
    }
    return best
}

@Composable
private fun FontScaleRow(
    fontScale: Float,
    onFontScaleChange: (Float) -> Unit
) {
    var sliderPosition by remember(fontScale) {
        mutableFloatStateOf(nearestFontScaleIndex(fontScale).toFloat())
    }
    val draggedScale = FONT_SCALE_STEPS[sliderPosition.roundToInt()]
    val previewStyle = MaterialTheme.typography.bodyLarge.let {
        it.copy(fontSize = it.fontSize * (draggedScale / fontScale))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.settings_font_size),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${(draggedScale * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = stringResource(R.string.settings_font_size_sample),
            style = previewStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            onValueChangeFinished = {
                onFontScaleChange(FONT_SCALE_STEPS[sliderPosition.roundToInt()])
            },
            valueRange = 0f..(FONT_SCALE_STEPS.size - 1).toFloat(),
            steps = FONT_SCALE_STEPS.size - 2
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppearanceScreenPreview() {
    MaterialTheme {
        AppearanceScreen(
            themeModeLabel = "System",
            accentLabel = "System",
            fontScale = 1.0f,
            showHabitSections = true,
            onNavigateUp = {}
        )
    }
}
