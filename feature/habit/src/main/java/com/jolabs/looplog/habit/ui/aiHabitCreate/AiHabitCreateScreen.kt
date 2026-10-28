@file:OptIn(
    ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)

package com.jolabs.looplog.habit.ui.aiHabitCreate

import android.app.Activity
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import com.jolabs.looplog.ui.components.AdaptiveCenteredContainer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.window.core.layout.WindowSizeClass
import com.jolabs.looplog.habit.R
import com.jolabs.looplog.habit.alarmManager.HabitAlarmManager
import com.jolabs.looplog.habit.ui.aiHabitCreate.components.AiDownloadProgressScreen
import com.jolabs.looplog.habit.ui.aiHabitCreate.components.AiDownloadSetupScreen
import com.jolabs.looplog.habit.ui.aiHabitCreate.components.AiHabitErrorState
import com.jolabs.looplog.habit.ui.aiHabitCreate.components.AiHabitEditBottomSheet
import com.jolabs.looplog.habit.ui.aiHabitCreate.components.AiHabitInput
import com.jolabs.looplog.habit.ui.aiHabitCreate.components.AiHabitLoading
import com.jolabs.looplog.habit.ui.aiHabitCreate.components.AiHabitQuickGoals
import com.jolabs.looplog.habit.ui.aiHabitCreate.components.AiNoticeScreen
import com.jolabs.looplog.habit.ui.aiHabitCreate.components.AiWaitingForWifiScreen
import com.jolabs.looplog.habit.ui.aiHabitCreate.components.EmptyHabitState
import com.jolabs.looplog.habit.ui.aiHabitCreate.components.GeneralAiError
import com.jolabs.looplog.habit.ui.aiHabitCreate.components.SuggestionCard
import java.time.DayOfWeek

@Preview(name = "Phone", device = Devices.PHONE, showBackground = true)
@Preview(name = "Foldable", device = Devices.FOLDABLE, showBackground = true)
@Preview(name = "Tablet", device = Devices.TABLET, showBackground = true)
@Preview(name = "Desktop", device = Devices.DESKTOP, showBackground = true)
annotation class FormFactorPreviews

data class AiHabitSuggestion(
    val id: String,
    val title: String,
    val description: String,
    val iconRes: Int,
    val selected: Boolean = false,
    val days: List<DayOfWeek> = DayOfWeek.entries.toList(),
    val timeOfDay: Long? = null,
)

enum class AiSetupState {
    Loading, AiNotice, DownloadSetup, DownloadingProgress, WaitingForWifi, Ready, Error, Unavailable
}

private const val MIN_QUERY_LENGTH_FOR_GENERATION = 4

data class AiHabitCreateUiState(
    val query: String = "",
    val suggestions: List<AiHabitSuggestion> = emptyList(),
    val isGenerating: Boolean = false,
    val isSaving: Boolean = false,
    val isError: Boolean = false,
    val isEnglishOnlyError: Boolean = false,
    val isIdle: Boolean = true,
    val setupState: AiSetupState = AiSetupState.Loading,
    val downloadProgress: Float = 0f,
) {
    val hasError: Boolean
        get() = isError || isEnglishOnlyError
}

@Composable
fun AiHabitCreateScreen(
    modifier: Modifier = Modifier,
    viewModel: AiHabitCreateViewModel = hiltViewModel(),
    onClose: () -> Unit = {},
    onAddHabits: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleStartEffect(viewModel) {
        viewModel.onForeground()
        onStopOrDispose { viewModel.onBackground() }
    }

    LaunchedEffect(uiState.setupState) {
        if (uiState.setupState == AiSetupState.Unavailable) onClose()
    }

    val wifiConfirmationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("AiHabitCreateScreen", "Mobile data download confirmed by user")
        }
    }

    BackHandler(enabled = (uiState.isGenerating || uiState.isSaving || uiState.setupState == AiSetupState.DownloadingProgress)) {
        // Do nothing to prevent back navigation while generating, saving or downloading
    }

    AiHabitCreateScreenInternal(
        uiState = uiState,
        onClose = onClose,
        onQueryChange = viewModel::onQueryChange,
        onGenerate = viewModel::generateSuggestions,
        onClear = viewModel::onClearQuery,
        onRetry = viewModel::resetError,
        onToggleSuggestion = viewModel::toggleSuggestion,
        onToggleDay = viewModel::toggleDay,
        onUpdateSuggestion = viewModel::updateSuggestion,
        onAddHabits = { viewModel.addSelectedHabits(onAddHabits) },
        onUseMobileData = { viewModel.triggerWifiConfirmation(wifiConfirmationLauncher) },
        acknowledgeAiNotice = viewModel::acknowledgeAiNotice,
        startDownload = viewModel::startDownload,
        clearAllState = viewModel::clearAllState,
        getHabitShapeWrapper = viewModel::getHabitShapeWrapper,
        habitAlarmManager = viewModel.habitAlarmManager,
        modifier = modifier
    )
}

@Composable
private fun AiHabitCreateScreenInternal(
    uiState: AiHabitCreateUiState,
    onClose: () -> Unit,
    onQueryChange: (String) -> Unit,
    onGenerate: () -> Unit,
    onClear: () -> Unit,
    onRetry: () -> Unit,
    onToggleSuggestion: (String) -> Unit,
    onToggleDay: (String, DayOfWeek) -> Unit,
    onUpdateSuggestion: (String, String, String, Long?) -> Unit,
    onAddHabits: () -> Unit,
    onUseMobileData: () -> Unit,
    acknowledgeAiNotice: () -> Unit,
    startDownload: () -> Unit,
    clearAllState: () -> Unit,
    habitAlarmManager: HabitAlarmManager? = null,
    getHabitShapeWrapper: (String) -> androidx.compose.ui.graphics.Shape = { CircleShape },
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = uiState.setupState,
        transitionSpec = {
            fadeIn(animationSpec = tween(600)) togetherWith fadeOut(animationSpec = tween(600))
        },
        label = "setup_state_transition"
    ) { state ->
        @OptIn(ExperimentalMaterial3ExpressiveApi::class)
        when (state) {
            AiSetupState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        LoadingIndicator(Modifier.size(48.dp))
                        Text(
                            text = stringResource(R.string.ai_habit_model_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            AiSetupState.AiNotice -> {
                AiNoticeScreen(
                    onDismiss = onClose,
                    onAcknowledge = acknowledgeAiNotice
                )
            }
            AiSetupState.DownloadSetup -> {
                AiDownloadSetupScreen(
                    onClose = onClose,
                    onDownloadClick = startDownload
                )
            }
            AiSetupState.DownloadingProgress -> {
                AiDownloadProgressScreen(
                    progress = uiState.downloadProgress,
                    onClose = onClose
                )
            }
            AiSetupState.WaitingForWifi -> {
                AiWaitingForWifiScreen(
                    onClose = onClose,
                    onUseMobileDataClick = onUseMobileData
                )
            }
            AiSetupState.Error -> {
                GeneralAiError(
                    onBack = {
                        clearAllState()
                        onClose()
                    }
                )
            }
            AiSetupState.Unavailable -> Unit
            AiSetupState.Ready -> {
                AiHabitCreateContent(
                    uiState = uiState,
                    onClose = onClose,
                    onQueryChange = onQueryChange,
                    onGenerate = onGenerate,
                    onClear = onClear,
                    onRetry = onRetry,
                    onToggleSuggestion = onToggleSuggestion,
                    onToggleDay = onToggleDay,
                    onUpdateSuggestion = onUpdateSuggestion,
                    onAddHabits = onAddHabits,
                    getHabitShapeWrapper = getHabitShapeWrapper,
                    habitAlarmManager = habitAlarmManager,
                    modifier = modifier
                )
            }
        }
    }
}

@Composable
private fun AiHabitCreateContent(
    uiState: AiHabitCreateUiState,
    onClose: () -> Unit,
    onQueryChange: (String) -> Unit,
    onGenerate: () -> Unit,
    onClear: () -> Unit,
    onRetry: () -> Unit,
    onToggleSuggestion: (String) -> Unit,
    onToggleDay: (String, DayOfWeek) -> Unit,
    onUpdateSuggestion: (String, String, String, Long?) -> Unit,
    onAddHabits: () -> Unit,
    getHabitShapeWrapper: (String) -> androidx.compose.ui.graphics.Shape,
    habitAlarmManager: HabitAlarmManager?,
    modifier: Modifier = Modifier
) {
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val isExpanded = adaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
    val isKeyboardVisible = WindowInsets.isImeVisible
    val canGenerate = uiState.query.length > MIN_QUERY_LENGTH_FOR_GENERATION
    var inputFocusRequestId by remember { mutableIntStateOf(0) }
    var isInputFocused by remember { mutableStateOf(false) }
    var editingSuggestionId by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val editingSuggestion = uiState.suggestions.firstOrNull { it.id == editingSuggestionId }
    val showQuickGoals = isKeyboardVisible &&
        isInputFocused &&
        uiState.query.isBlank() &&
        uiState.suggestions.isEmpty() &&
        !uiState.hasError &&
        !uiState.isGenerating
    val retryAndFocusInput: () -> Unit = {
        onRetry()
        inputFocusRequestId++
    }

    if (editingSuggestion != null) {
        AiHabitEditBottomSheet(
            suggestion = editingSuggestion,
            habitAlarmManager = habitAlarmManager,
            snackbarHostState = snackbarHostState,
            onDismiss = { editingSuggestionId = null },
            onSave = { title, description, timeOfDay ->
                onUpdateSuggestion(editingSuggestion.id, title, description, timeOfDay)
                editingSuggestionId = null
            },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AdaptiveCenteredContainer(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        // Empty spacer to match design which has close on right
                        Spacer(Modifier.width(48.dp))
                    },
                    actions = {
                        val iconAlpha = if (uiState.isGenerating) 0.5f else 1f
                        Icon(
                            painter = painterResource(id = R.drawable.close),
                            contentDescription = stringResource(R.string.ai_habit_close),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(enabled = !uiState.isGenerating) { onClose() }
                                .padding(12.dp)
                                .size(28.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = iconAlpha)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        bottomBar = {
            val selectedCount = uiState.suggestions.count { it.selected }
            AdaptiveCenteredContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        , horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (selectedCount > 0) {
                        Button(
                            onClick = onAddHabits,
                            enabled = !uiState.isSaving,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            if (uiState.isSaving) {
                                LoadingIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = pluralStringResource(
                                            R.plurals.ai_habit_add_selected,
                                            selectedCount,
                                            selectedCount,
                                        ),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Icon(
                                        painter = painterResource(id = R.drawable.chevron_right),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        AnimatedVisibility(
                            visible = showQuickGoals,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            Column {
                                AiHabitQuickGoals(
                                    onGoalClick = { goal ->
                                        onQueryChange(goal)
                                        onGenerate()
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                        AiHabitInput(
                            query = uiState.query,
                            onQueryChange = onQueryChange,
                            onGenerate = {
                                if (!uiState.isGenerating && canGenerate) onGenerate()
                            },
                            onClear = onClear,
                            isGenerating = uiState.isGenerating,
                            canGenerate = canGenerate,
                            showClose = uiState.suggestions.isNotEmpty() || uiState.hasError,
                            focusRequestKey = inputFocusRequestId,
                            onFocusChange = { isInputFocused = it },
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.ai_habit_suggestions_disclaimer),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        AdaptiveCenteredContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (uiState.suggestions.isNotEmpty()) {
                            AiHabitSuggestions(
                                suggestions = uiState.suggestions,
                                isExpanded = isExpanded,
                                getHabitShapeWrapper = getHabitShapeWrapper,
                                onToggleSuggestion = onToggleSuggestion,
                                onToggleDay = onToggleDay,
                                onEditSuggestion = { editingSuggestionId = it },
                            )
                        } else {
                            when {
                                uiState.isGenerating -> AiHabitLoading(modifier = Modifier.fillMaxSize())
                                uiState.isEnglishOnlyError -> AiHabitErrorState(
                                    errorTitle = stringResource(R.string.ai_habit_english_only_title),
                                    errorMessage = stringResource(R.string.ai_habit_english_only_error),
                                    onRetryClick = retryAndFocusInput,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                uiState.isError -> AiHabitErrorState(
                                    onRetryClick = retryAndFocusInput,
                                    modifier = Modifier.fillMaxSize()
                                )
                                else -> EmptyHabitState(
                                    showContent = !isKeyboardVisible,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiHabitSuggestions(
    suggestions: List<AiHabitSuggestion>,
    isExpanded: Boolean,
    getHabitShapeWrapper: (String) -> androidx.compose.ui.graphics.Shape,
    onToggleSuggestion: (String) -> Unit,
    onToggleDay: (String, DayOfWeek) -> Unit,
    onEditSuggestion: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = if (isExpanded) GridCells.Fixed(2) else GridCells.Fixed(1),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        stickyHeader {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(bottom = 8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.start),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.ai_habit_suggestions_generated),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        items(suggestions, key = { it.id }) { suggestion ->
            SuggestionCard(
                suggestion = suggestion,
                shape = getHabitShapeWrapper(suggestion.id),
                onClick = { onToggleSuggestion(suggestion.id) },
                onDayToggle = { day -> onToggleDay(suggestion.id, day) },
                onEdit = { onEditSuggestion(suggestion.id) },
            )
        }
    }
}

@FormFactorPreviews
@Composable
private fun AiHabitSuggestionsPreview() {
    AiHabitScreenPreview(
        AiHabitCreateUiState(
            query = "Build a better morning routine",
            suggestions = listOf(
                AiHabitSuggestion("1", "Drink Water", "Drink 8 glasses of water", R.drawable.start, selected = true),
                AiHabitSuggestion("2", "Read Book", "Read 10 pages of a book", R.drawable.start, selected = false)
            ),
            setupState = AiSetupState.Ready
        )
    )
}

@Preview(name = "Idle", showBackground = true)
@Composable
private fun AiHabitIdlePreview() {
    AiHabitScreenPreview(AiHabitCreateUiState(setupState = AiSetupState.Ready))
}

@Preview(name = "Thinking", showBackground = true)
@Composable
private fun AiHabitThinkingPreview() {
    AiHabitScreenPreview(
        AiHabitCreateUiState(
            query = "Sleep better",
            isGenerating = true,
            isIdle = false,
            setupState = AiSetupState.Ready
        )
    )
}

@Preview(name = "Error", showBackground = true)
@Composable
private fun AiHabitErrorPreview() {
    AiHabitScreenPreview(
        AiHabitCreateUiState(
            query = "Sleep better",
            isError = true,
            isIdle = false,
            setupState = AiSetupState.Ready
        )
    )
}

@Composable
private fun AiHabitScreenPreview(uiState: AiHabitCreateUiState) {
    MaterialTheme {
        AiHabitCreateScreenInternal(
            uiState = uiState,
            onClose = {},
            onQueryChange = {},
            onGenerate = {},
            onClear = {},
            onRetry = {},
            onToggleSuggestion = {},
            onToggleDay = { _, _ -> },
            onUpdateSuggestion = { _, _, _, _ -> },
            onAddHabits = {},
            onUseMobileData = {},
            acknowledgeAiNotice = {},
            startDownload = {},
            clearAllState = {},
            habitAlarmManager = null,
            getHabitShapeWrapper = { CircleShape }
        )
    }
}
