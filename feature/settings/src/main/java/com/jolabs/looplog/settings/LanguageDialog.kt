package com.jolabs.looplog.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jolabs.looplog.settings.R

/**
 * A selectable app language. [tag] is a BCP-47 tag, or null for the system default. Labels are
 * shown as endonyms (each language in its own script) except the system default, which is
 * localized. Tags mirror res/xml/locales_config.xml.
 */
data class LanguageOption(val tag: String?, val label: String)

/** The supported languages, system default first. */
@Composable
fun rememberLanguageOptions(): List<LanguageOption> = listOf(
    LanguageOption(null, stringResource(R.string.language_system_default)),
    LanguageOption("en", "English"),
    LanguageOption("hi", "हिन्दी"),
    LanguageOption("in", "Bahasa Indonesia")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageDialog(
    options: List<LanguageOption>,
    selectedTag: String?,
    onSelect: (String?) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_app_language),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            options.forEach { option ->
                LanguageOptionRow(
                    label = option.label,
                    selected = option.tag == selectedTag,
                    onClick = { onSelect(option.tag) }
                )
            }
        }
    }
}

@Composable
private fun LanguageOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
