package com.jolabs.looplog.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jolabs.looplog.settings.R
import com.jolabs.looplog.design_system.ui.theme.accentSwatchColor
import com.jolabs.looplog.model.AppAccent

/** Bottom sheet with a swatch grid for choosing the accent color. Tapping applies immediately. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AccentColorDialog(
    selectedAccent: AppAccent,
    onSelect: (AppAccent) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 48.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_accent_color),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppAccent.entries.forEach { accent ->
                    AccentSwatch(
                        accent = accent,
                        selected = accent == selectedAccent,
                        onClick = { onSelect(accent) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AccentSwatch(
    accent: AppAccent,
    selected: Boolean,
    onClick: () -> Unit
) {
    val swatchColor = accentSwatchColor(accent) ?: MaterialTheme.colorScheme.surfaceVariant
    val borderColor =
        if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = Modifier.width(56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(swatchColor)
                .border(width = if (selected) 3.dp else 1.dp, color = borderColor, shape = CircleShape)
                .clickable(onClick = onClick)
        ) {}
        Text(
            text = accentLabel(accent),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun accentLabel(accent: AppAccent): String = stringResource(
    when (accent) {
        AppAccent.SYSTEM -> R.string.accent_system
        AppAccent.BLUE -> R.string.accent_blue
        AppAccent.VIOLET -> R.string.accent_violet
        AppAccent.TEAL -> R.string.accent_teal
        AppAccent.AMBER -> R.string.accent_amber
        AppAccent.ROSE -> R.string.accent_rose
    }
)
