package com.jolabs.looplog.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jolabs.looplog.settings.components.SettingsLinkRow

/** Entry points for exporting or importing app data. */
@Composable
fun BackupRestoreSheet(
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onDismiss: () -> Unit
) {
    SettingsActionsSheet(
        title = stringResource(R.string.settings_section_backup),
        onDismiss = onDismiss
    ) {
        SettingsLinkRow(
            icon = R.drawable.backup,
            label = stringResource(R.string.settings_export),
            description = stringResource(R.string.settings_export_desc),
            onClick = onExportClick
        )
        SettingsLinkRow(
            icon = R.drawable.backup,
            label = stringResource(R.string.settings_import),
            description = stringResource(R.string.settings_import_desc),
            onClick = onImportClick
        )
    }
}

/** Links to looplog's legal documents. */
@Composable
fun PrivacyTermsSheet(
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onDismiss: () -> Unit
) {
    SettingsActionsSheet(
        title = stringResource(R.string.settings_section_privacy),
        onDismiss = onDismiss
    ) {
        SettingsLinkRow(
            icon = R.drawable.description,
            label = stringResource(R.string.settings_terms),
            onClick = onTermsClick
        )
        SettingsLinkRow(
            icon = R.drawable.privacy_lock,
            label = stringResource(R.string.settings_privacy_policy),
            onClick = onPrivacyClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsActionsSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, bottom = 48.dp, top = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 8.dp)
            )
            content()
        }
    }
}
