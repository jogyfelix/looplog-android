package com.jolabs.looplog.habit.ui.aiHabitCreate.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.jolabs.looplog.habit.R

@Composable
fun AiHabitInput(
    query: String,
    onQueryChange: (String) -> Unit,
    onGenerate: () -> Unit,
    onClear: () -> Unit,
    isGenerating: Boolean = false,
    canGenerate: Boolean = false,
    showClose: Boolean = false,
    focusRequestKey: Int = 0,
    onFocusChange: (Boolean) -> Unit = {},
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val isInputEnabled = !isGenerating && !showClose
    var isExpanded by remember { mutableStateOf(false) }
    var expansionThreshold by remember { mutableIntStateOf(Int.MAX_VALUE) }

    LaunchedEffect(query) {
        if (query.isEmpty()) {
            isExpanded = false
            expansionThreshold = Int.MAX_VALUE
        }
    }

    LaunchedEffect(focusRequestKey) {
        if (focusRequestKey > 0) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    BasicTextField(
        value = query,
        onValueChange = { value ->
            val updatedQuery = value.take(80)
            if (isExpanded && updatedQuery.length < expansionThreshold) {
                isExpanded = false
                expansionThreshold = Int.MAX_VALUE
            }
            onQueryChange(updatedQuery)
        },
        enabled = isInputEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp, max = 200.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { onFocusChange(it.isFocused) }
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Default
        ),
        textStyle = MaterialTheme.typography.titleLarge.copy(
            color = MaterialTheme.colorScheme.onSurface.copy(
                alpha = if (isInputEnabled) 1f else 0.38f,
            )
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        singleLine = false,
        minLines = 1,
        maxLines = 5,
        onTextLayout = { textLayoutResult ->
            if (!isExpanded && textLayoutResult.lineCount > 1) {
                expansionThreshold = query.length.coerceAtLeast(1)
                isExpanded = true
            }
        },
        decorationBox = { innerTextField ->
            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                ) {
                    AiHabitInputText(
                        query = query,
                        innerTextField = innerTextField,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AiHabitInputWand()
                        Spacer(modifier = Modifier.weight(1f))
                        AiHabitInputActions(
                            query = query,
                            showClose = showClose,
                            canGenerate = canGenerate,
                            isGenerating = isGenerating,
                            onClear = onClear,
                            onGenerate = onGenerate,
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    AiHabitInputWand()
                    Spacer(modifier = Modifier.width(4.dp))
                    AiHabitInputText(
                        query = query,
                        innerTextField = innerTextField,
                        modifier = Modifier.weight(1f),
                    )
                    AiHabitInputActions(
                        query = query,
                        showClose = showClose,
                        canGenerate = canGenerate,
                        isGenerating = isGenerating,
                        onClear = onClear,
                        onGenerate = onGenerate,
                        modifier = Modifier.heightIn(min = 40.dp),
                    )
                }
            }
        },
    )
}

@Composable
private fun AiHabitInputWand() {
    Box(
        modifier = Modifier.size(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.wand),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun AiHabitInputText(
    query: String,
    innerTextField: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.heightIn(min = 40.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (query.isEmpty()) {
            Text(
                text = stringResource(R.string.ai_habit_input_placeholder),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }
        innerTextField()
    }
}

@Composable
private fun AiHabitInputActions(
    query: String,
    showClose: Boolean,
    canGenerate: Boolean,
    isGenerating: Boolean,
    onClear: () -> Unit,
    onGenerate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (query.isNotEmpty()) {
            Text(
                text = "${query.length}/80",
                style = MaterialTheme.typography.labelSmall,
                color = if (query.length >= 80) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                },
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        if (showClose) {
            Icon(
                painter = painterResource(id = R.drawable.close),
                contentDescription = "Clear",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onClear() }
                    .padding(10.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val isGenerateEnabled = canGenerate && !isGenerating
            val containerColor = when {
                isGenerating -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                isGenerateEnabled -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            }
            Icon(
                painter = painterResource(id = R.drawable.chevron_right),
                contentDescription = "Generate",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(containerColor)
                    .clickable(enabled = isGenerateEnabled) { onGenerate() }
                    .padding(8.dp),
                tint = MaterialTheme.colorScheme.onPrimary.copy(
                    alpha = if (isGenerateEnabled) 1f else 0.6f
                )
            )
        }
    }
}
