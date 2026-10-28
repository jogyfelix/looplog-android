package com.jolabs.looplog.habit.ui.aiHabitCreate.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun AiHabitLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row {
            Text(
                text = "Thinking",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.alignByBaseline(),
            )
            ThinkingDots(
                modifier = Modifier.alignBy { measured ->
                    measured.measuredHeight * 3 / 4
                },
            )
        }
        Text(
            text = "Generating personalized habits securely on your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun ThinkingDots(modifier: Modifier = Modifier) {
    var visibleDots by remember { mutableIntStateOf(1) }
    val activeColor = MaterialTheme.colorScheme.onSurface
    val inactiveColor = activeColor.copy(alpha = 0.2f)

    LaunchedEffect(Unit) {
        while (true) {
            delay(600L.milliseconds)
            visibleDots = visibleDots % DOT_COUNT + 1
        }
    }

    Canvas(
        modifier = modifier
            .width(26.dp)
            .height(10.dp)
    ) {
        val dotRadius = size.height / 4f
        val spacing = size.width / DOT_COUNT

        repeat(DOT_COUNT) { index ->
            drawCircle(
                color = if (index < visibleDots) activeColor else inactiveColor,
                radius = dotRadius,
                center = Offset(
                    x = spacing * index + spacing / 2f,
                    y = size.height / 2f
                )
            )
        }
    }
}

private const val DOT_COUNT = 3

@Preview(showBackground = true, name = "AI Loading")
@Composable
private fun AiHabitLoadingPreview() {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AiHabitLoading()
        }
    }
}
