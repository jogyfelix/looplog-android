package com.jolabs.looplog.habit.ui.aiHabitCreate.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jolabs.looplog.habit.R

@Composable
fun AiHabitErrorState(
    modifier: Modifier = Modifier,
    errorTitle: String? = null,
    errorMessage: String? = null,
    onRetryClick: () -> Unit
) {
    val resolvedTitle = errorTitle ?: stringResource(R.string.ai_habit_error_title)
    val resolvedMessage = errorMessage ?: stringResource(R.string.ai_habit_error_body)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Visual element matching the empty/loading structural style
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(120.dp)
        ) {
            // Subtle background circle using the error color scheme
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
            )

            // Warning/Error Icon (Swap R.drawable.warning with your preferred icon)
            Icon(
                painter = painterResource(id = R.drawable.error),
                contentDescription = stringResource(R.string.ai_habit_error_content_description),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Primary error state message
        Text(
            text = resolvedTitle,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Secondary explanatory text (configurable via parameters)
        Text(
            text = resolvedMessage,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Retry Action
        Button(
            onClick = onRetryClick,
            modifier = Modifier.height(48.dp)
        ) {
            Text(text = stringResource(R.string.ai_habit_error_try_again))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AiHabitError() {
    AiHabitErrorState() { }
    
}
