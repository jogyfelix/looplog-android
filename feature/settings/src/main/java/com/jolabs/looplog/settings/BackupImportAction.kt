package com.jolabs.looplog.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.jolabs.looplog.settings.backup.BackupEvent
import com.jolabs.looplog.settings.backup.BackupViewModel

@Composable
fun rememberBackupImportAction(
    backupViewModel: BackupViewModel = hiltViewModel()
): () -> Unit {
    val context = LocalContext.current.applicationContext
    var showPasswordSheet by remember { mutableStateOf(false) }
    var showConfirmSheet by remember { mutableStateOf(false) }

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) backupViewModel.onImportFilePicked(uri) }
    val importAction = remember(fileLauncher) {
        { fileLauncher.launch(arrayOf("application/octet-stream", "*/*")) }
    }

    LaunchedEffect(backupViewModel) {
        backupViewModel.events.collect { event ->
            fun toast(resId: Int) = Toast.makeText(
                context,
                context.getString(resId),
                Toast.LENGTH_LONG
            ).show()

            when (event) {
                BackupEvent.ExportSucceeded, BackupEvent.ExportFailed -> Unit
                BackupEvent.ImportNeedsPassword -> showPasswordSheet = true
                BackupEvent.ImportReadyToConfirm -> {
                    showPasswordSheet = false
                    showConfirmSheet = true
                }
                BackupEvent.ImportNotABackup -> {
                    showConfirmSheet = false
                    toast(R.string.backup_import_not_a_backup)
                }
                BackupEvent.ImportWrongPassword -> {
                    showConfirmSheet = false
                    toast(R.string.backup_import_wrong_password)
                    showPasswordSheet = true
                }
                is BackupEvent.ImportIncompatible -> {
                    showConfirmSheet = false
                    toast(R.string.backup_import_incompatible)
                }
                is BackupEvent.ImportSucceeded -> {
                    showConfirmSheet = false
                    toast(R.string.backup_import_success)
                }
                BackupEvent.ImportFailed -> {
                    showConfirmSheet = false
                    toast(R.string.backup_import_failed)
                }
            }
        }
    }

    if (showPasswordSheet) {
        ImportPasswordSheet(
            onSubmit = backupViewModel::onImportPasswordEntered,
            onDismiss = {
                showPasswordSheet = false
                backupViewModel.cancelImport()
            }
        )
    }

    if (showConfirmSheet) {
        ConfirmRestoreSheet(
            onConfirm = {
                showConfirmSheet = false
                backupViewModel.confirmImport()
            },
            onDismiss = {
                showConfirmSheet = false
                backupViewModel.cancelImport()
            }
        )
    }

    return importAction
}
