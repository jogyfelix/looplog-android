package com.jolabs.looplog.habit.ui.components

import android.annotation.SuppressLint
import android.graphics.RectF
import android.graphics.Matrix
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import com.jolabs.looplog.design_system.ui.theme.RoundedPolygonShape
import com.jolabs.looplog.habit.R
import com.jolabs.looplog.model.DailyCompletionState
import com.jolabs.looplog.model.DailyStat
import java.time.LocalDate
import java.time.format.TextStyle

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WeekAtAGlanceCard(
    weeklyStats: List<DailyStat>, // Should be exactly 7 items (Last 7 days, right-most is today)
    isAnimated: Boolean,          // Hoisted from ViewModel — stays true once played, preventing replays on forward navigation
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.medium
            )
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.habit_last_7_days),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            // maxWidth here is already the inner content width (Column padding already consumed)
            val shapeSize = (maxWidth / 7 - 6.dp).coerceIn(44.dp, 72.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyStats.forEachIndexed { index, stat ->
                    DayMorphShape(
                        stat = stat,
                        size = shapeSize,
                        isAnimated = isAnimated,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Micro-Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(
                text = stringResource(R.string.status_skipped),
                color = MaterialTheme.colorScheme.outlineVariant, // A softer grey for the dot
                shape = RoundedPolygonShape(MaterialShapes.Circle),
                iconSize = 6.dp // Scaled down for the legend
            )
            Spacer(modifier = Modifier.size(16.dp))
            LegendItem(
                text = stringResource(R.string.habit_partial),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedPolygonShape(MaterialShapes.Arch)
            )
            Spacer(modifier = Modifier.size(16.dp))
            LegendItem(
                text = stringResource(R.string.habit_perfect),
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedPolygonShape(MaterialShapes.Cookie4Sided)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DayMorphShape(
    stat: DailyStat,
    size: Dp,
    isAnimated: Boolean,
    modifier: Modifier = Modifier
) {
    // 1. Define Target Shapes based on state
    val targetPolygon = remember(stat.state) {
        when (stat.state) {
            DailyCompletionState.NONE -> MaterialShapes.Circle // The dot is a circle
            DailyCompletionState.PARTIAL -> MaterialShapes.Arch
            DailyCompletionState.PERFECT -> MaterialShapes.Cookie4Sided
        }
    }

    // 2. Base Circle for the entry animation
    val startPolygon = remember { MaterialShapes.Circle }

    // 3. Create the Morph object
    val morph = remember(targetPolygon) {
        Morph(startPolygon, targetPolygon)
    }

    // 4. Animate the morph progress
    val morphProgress by animateFloatAsState(
        targetValue = if (isAnimated) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "ShapeMorph"
    )

    // 5. Determine scaling based on state (None shrinks to a dot)
    val targetScale = when (stat.state) {
        DailyCompletionState.NONE -> 0.35f // Shrink to 35% size
        else -> 1f // Full size
    }

    // Animate the scale so it smoothly shrinks or grows on entry
    val scale by animateFloatAsState(
        targetValue = if (isAnimated) targetScale else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "ShapeScale"
    )

    // 6. Determine styling based on state
    val shapeColor = when (stat.state) {
        DailyCompletionState.NONE -> MaterialTheme.colorScheme.outlineVariant // Softer grey for the empty dot
        DailyCompletionState.PARTIAL -> MaterialTheme.colorScheme.primaryContainer
        DailyCompletionState.PERFECT -> MaterialTheme.colorScheme.primary
    }

    val locale = LocalConfiguration.current.locales[0]
    val dayLabel = stat.date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // The Morphing Shape
        Box(
            modifier = Modifier
                .size(size) // This reserves the full layout space so items don't shift
                .scale(scale) // This visually scales the shape up/down
                .clip(MorphShape(morph, morphProgress))
                .background(shapeColor)
        )

        // Rolling Day Label
        Text(
            text = dayLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LegendItem(
    text: String,
    color: Color,
    shape: Shape,
    iconSize: Dp = 12.dp // Default to 12.dp, override for the Dot
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Outer box keeps the alignment consistent even if iconSize changes
        Box(
            modifier = Modifier.size(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .background(color, shape)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF121212) // Dark background for neobrutalism feel
@Composable
private fun WeekAtAGlanceCardPreview() {
    MaterialTheme {
        // Mocking the last 7 days ending on a Thursday
        val today = LocalDate.now()

        val mockData = listOf(
            DailyStat(date = today.minusDays(6), state = DailyCompletionState.NONE),    // Friday
            DailyStat(date = today.minusDays(5), state = DailyCompletionState.PARTIAL), // Saturday
            DailyStat(date = today.minusDays(4), state = DailyCompletionState.PERFECT), // Sunday
            DailyStat(date = today.minusDays(3), state = DailyCompletionState.PARTIAL), // Monday
            DailyStat(date = today.minusDays(2), state = DailyCompletionState.NONE),    // Tuesday
            DailyStat(date = today.minusDays(1), state = DailyCompletionState.PERFECT), // Wednesday
            DailyStat(date = today, state = DailyCompletionState.PARTIAL)               // Thursday (Today)
        )

        Box(modifier = Modifier.padding(16.dp)) {
            WeekAtAGlanceCard(
                weeklyStats = mockData,
                isAnimated = true
            )
        }
    }
}


// --- Utility: Morph Shape Wrapper ---
class MorphShape(
    private val morph: Morph,
    private val progress: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        // 1. Get the raw path in its original coordinate space
        val composePath = morph.toPath(progress = progress).asComposePath()

        // 2. Get the actual bounds of the generated shape
        val bounds = composePath.getBounds()

        // 3. Map the shape's native bounds exactly to the Compose Box bounds
        val matrix = Matrix()
        val srcRect = RectF(bounds.left, bounds.top, bounds.right, bounds.bottom)
        val dstRect = RectF(0f, 0f, size.width, size.height)

        // ScaleToFit.CENTER stretches the shape to fill the box while keeping it perfectly centered
        matrix.setRectToRect(srcRect, dstRect, Matrix.ScaleToFit.CENTER)

        // 4. Apply the matrix transformation
        composePath.asAndroidPath().transform(matrix)

        return Outline.Generic(composePath)
    }
}

