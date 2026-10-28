package com.jolabs.looplog.habit.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.jolabs.looplog.habit.R
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields
import kotlin.math.roundToInt

// --- STATE HOLDER ---

@Stable
class ExpandableCalendarState(initialExpanded: Boolean = false) {
    var isExpanded by mutableStateOf(initialExpanded)
    fun collapse() { isExpanded = false }
    fun expand()   { isExpanded = true  }

    companion object {
        val Saver = Saver<ExpandableCalendarState, Boolean>(
            save    = { it.isExpanded },
            restore = { ExpandableCalendarState(it) }
        )
    }
}

@Composable
fun rememberExpandableCalendarState(initialExpanded: Boolean = false): ExpandableCalendarState =
    rememberSaveable(saver = ExpandableCalendarState.Saver) {
        ExpandableCalendarState(initialExpanded)
    }

// --- CONSTANTS ---
private val CELL_HEIGHT = 52.dp
private val DAY_LABEL_HEIGHT = 24.dp
private val HEADER_HEIGHT = 50.dp
private val DRAG_HANDLE_HEIGHT = 32.dp
private val VERTICAL_SPACING = 8.dp
private val TODAY_CHIP_HEIGHT = 48.dp

@Composable
fun ExpandableCalendar(
    modifier: Modifier = Modifier,
    selectedDate: LocalDate = LocalDate.now(),
    state: ExpandableCalendarState = rememberExpandableCalendarState(),
    allowExpansion: Boolean = true,
    externalPullOffset: Float = 0f,
    onDateSelected: (LocalDate) -> Unit = {},
) {
    val density = LocalDensity.current

    // 1. HEIGHT CALCULATIONS
    val compactHeight = remember(density) {
        DAY_LABEL_HEIGHT + 10.dp + CELL_HEIGHT + DRAG_HANDLE_HEIGHT + 8.dp
    }

    val expandedHeight = remember(density) {
        HEADER_HEIGHT + DAY_LABEL_HEIGHT +
                (CELL_HEIGHT * 6) + (VERTICAL_SPACING * 5) +
                TODAY_CHIP_HEIGHT + DRAG_HANDLE_HEIGHT
    }

    val dragDistancePx = with(density) { (expandedHeight - compactHeight).toPx() }
    val safeDragDistance = if (dragDistancePx == 0f) 1f else dragDistancePx

    var dragOffsetY by remember {
        mutableFloatStateOf(if (state.isExpanded) safeDragDistance else 0f)
    }

    // Apply external pull offset from parent (for pull-to-expand gesture)
    LaunchedEffect(externalPullOffset) {
        if (externalPullOffset > 0 && !state.isExpanded) {
            dragOffsetY = externalPullOffset.coerceIn(0f, safeDragDistance)
        }
    }

    // Animate dragOffsetY whenever isExpanded changes (covers both drag and external collapse)
    LaunchedEffect(state.isExpanded, safeDragDistance) {
        // Only animate if no external pull is active
        if (externalPullOffset == 0f) {
            androidx.compose.animation.core.animate(
                initialValue = dragOffsetY,
                targetValue = if (state.isExpanded) safeDragDistance else 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) { value, _ -> dragOffsetY = value }
        }
    }

    val progress = (dragOffsetY / safeDragDistance).coerceIn(0f, 1f)
    val currentHeight = lerp(compactHeight, expandedHeight, progress)
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(currentHeight)
            .background(Color.Transparent)
            .clipToBounds()
            .then(
                if (allowExpansion) {
                    Modifier.draggable(
                        state = rememberDraggableState { delta ->
                            dragOffsetY = (dragOffsetY + delta).coerceIn(0f, safeDragDistance)
                        },
                        orientation = Orientation.Vertical,
                        onDragStopped = { velocity ->
                            val targetExpanded = when {
                                velocity > 1000f -> true
                                velocity < -1000f -> false
                                else -> progress > 0.5f
                            }
                            state.isExpanded = targetExpanded
                            scope.launch {
                                androidx.compose.animation.core.animate(
                                    initialValue = dragOffsetY,
                                    targetValue = if (targetExpanded) safeDragDistance else 0f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                ) { value, _ -> dragOffsetY = value }
                            }
                        }
                    )
                } else {
                    Modifier
                }
            )
    ) {
        CalendarContent(
            selectedDate = selectedDate,
            progress = progress,
            onDateSelected = onDateSelected,
            modifier = Modifier.align(Alignment.TopCenter)
        )

      if(allowExpansion){
          DragHandleIndicator(
              modifier = Modifier
                  .align(Alignment.BottomCenter)
                  .height(DRAG_HANDLE_HEIGHT)
                  .fillMaxWidth()
          )
      }
    }
}

@Composable
private fun CalendarContent(
    selectedDate: LocalDate,
    progress: Float,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalLocale.current.platformLocale
    val initialMonth = remember { YearMonth.from(selectedDate) }
    val monthRange = 240
    val centerIndex = monthRange / 2
    val selectedMonth = YearMonth.from(selectedDate)

    val targetPage = centerIndex +
            ((selectedMonth.year - initialMonth.year) * 12 +
                    (selectedMonth.monthValue - initialMonth.monthValue))

    val pagerState = rememberPagerState(initialPage = targetPage) { monthRange }
    val currentViewMonth = initialMonth.plusMonths((pagerState.currentPage - centerIndex).toLong())
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxWidth()) {

        // TOP PADDING (only when collapsed and topbar hidden)
        val topPadding = lerp(16.dp, 0.dp, progress)
        Spacer(modifier = Modifier.height(topPadding))

        // HEADER
        val currentHeaderHeight = lerp(0.dp, HEADER_HEIGHT, progress)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(currentHeaderHeight)
                .graphicsLayer { alpha = progress },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${currentViewMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${currentViewMonth.year}",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        // LABELS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(DAY_LABEL_HEIGHT)
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val weekFields = WeekFields.of(locale)
            (0..6).forEach { i ->
                Text(
                    text = weekFields.firstDayOfWeek.plus(i.toLong()).name.take(3),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )
            }
        }

        // GRID CONTAINER
        // Fixed height Box ensures the grid has room to render rows at correct offsets
        val gridHeight = (CELL_HEIGHT * 6) + (VERTICAL_SPACING * 5)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight)
                .clipToBounds() // Prevents rows from overlapping labels
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Top,
                userScrollEnabled = progress > 0.8f
            ) { page ->
                val displayMonth = initialMonth.plusMonths((page - centerIndex).toLong())
                SingleLayerMonthGrid(
                    yearMonth = displayMonth,
                    selectedDate = selectedDate,
                    progress = progress,
                    onDateSelected = onDateSelected
                )
            }
        }

        // TODAY CHIP
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TODAY_CHIP_HEIGHT)
                .padding(top = 8.dp),
            contentAlignment = Alignment.TopStart
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = currentViewMonth != YearMonth.now() && progress > 0.8f,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
            ) {
                AssistChip(
                    onClick = {
                        val today = LocalDate.now()
                        val page = centerIndex + ((YearMonth.from(today).year - initialMonth.year) * 12 + (YearMonth.from(today).monthValue - initialMonth.monthValue))
                        scope.launch { pagerState.animateScrollToPage(page) }
                        onDateSelected(today)
                    },
                    label = { Text(stringResource(R.string.today)) },
                    colors = AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.primary)
                )
            }
        }

        Spacer(modifier = Modifier.height(DRAG_HANDLE_HEIGHT))
    }
}

@Composable
private fun SingleLayerMonthGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    progress: Float,
    onDateSelected: (LocalDate) -> Unit
) {
    val weekFields = WeekFields.of(LocalLocale.current.platformLocale)
    val density = LocalDensity.current

    val firstDayOfMonth = yearMonth.atDay(1)
    val dayOfWeekOffset = firstDayOfMonth.get(weekFields.dayOfWeek()) - 1
    val startOfGrid = firstDayOfMonth.minusDays(dayOfWeekOffset.toLong())

    // 1. FIND THE ANCHOR ROW
    val selectedRowIndex = remember(selectedDate, yearMonth) {
        var foundRow = 0
        // Search all 42 cells to find the row index of the selected date
        for (i in 0 until 42) {
            val dateInCell = startOfGrid.plusDays(i.toLong())
            if (dateInCell == selectedDate) {
                foundRow = i / 7
                break
            }
        }
        foundRow
    }

    // 2. BOX LAYOUT: We manually position each row using offsets
    Box(modifier = Modifier.fillMaxSize()) {
        repeat(6) { rowIndex ->
            val isSelectedRow = rowIndex == selectedRowIndex
            val rowAlpha = if (isSelectedRow) 1f else progress

            // Calculate absolute Y position in Expanded state
            val rowHeightPx = with(density) { (CELL_HEIGHT + VERTICAL_SPACING).toPx() }
            val expandedY = rowIndex * rowHeightPx

            // Calculate Y position in Collapsed state
            // If this is the selected row, it goes to 0.
            // Others maintain relative distance.
            val collapsedY = expandedY - (selectedRowIndex * rowHeightPx)

            // Interpolate
            val currentY = androidx.compose.ui.util.lerp(collapsedY, expandedY, progress)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, currentY.roundToInt()) } // Use offset, not translation
                    .graphicsLayer { alpha = rowAlpha },
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(7) { colIndex ->
                    val cellIndex = rowIndex * 7 + colIndex
                    val cellDate = startOfGrid.plusDays(cellIndex.toLong())
                    val isCurrentMonth = YearMonth.from(cellDate) == yearMonth

                    DayCell(
                        date = cellDate,
                        isSelected = cellDate == selectedDate,
                        isCurrentMonth = isCurrentMonth,
                        isToday = cellDate == LocalDate.now(),
                        onDateClick = { onDateSelected(cellDate) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isCurrentMonth: Boolean,
    isToday: Boolean,
    onDateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isFuture = date.isAfter(LocalDate.now())

    Box(
        modifier = modifier
            .requiredHeight(CELL_HEIGHT)
            .clip(RoundedCornerShape(12.dp))
            .background(
                 MaterialTheme.colorScheme.surface
            )
            .border(
                width = if (isSelected || isToday) 1.5.dp else 1.dp,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    !isCurrentMonth -> Color.Transparent
                    else -> MaterialTheme.colorScheme.outlineVariant
                },
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = !isFuture) { onDateClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    isFuture -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    !isCurrentMonth -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            if (isToday && !isSelected) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
private fun DragHandleIndicator(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
    }
}
