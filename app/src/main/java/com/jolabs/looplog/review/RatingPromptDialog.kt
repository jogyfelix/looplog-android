package com.jolabs.looplog.review

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jolabs.looplog.R

/**
 * One-time nudge asking whether the user is enjoying the app. "Yes" opens the Play Store
 * rating page; "No" opens the UserJot feedback form.
 */
@Composable
fun RatingPromptDialog(
    onYes: () -> Unit,
    onNo: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rating_prompt_title)) },
        text = { Text(stringResource(R.string.rating_prompt_message)) },
        confirmButton = {
            TextButton(onClick = onYes) { Text(stringResource(R.string.rating_prompt_yes)) }
        },
        dismissButton = {
            TextButton(onClick = onNo) { Text(stringResource(R.string.rating_prompt_no)) }
        }
    )
}
