package com.jolabs.looplog.habit.ui.habitStats.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jolabs.looplog.design_system.ui.theme.RoundedPolygonShape
import com.jolabs.looplog.habit.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PerfectDaysCard(
    count: Int,
    modifier: Modifier = Modifier
) {
    // We reuse the Cookie shape from your Morphing logic for visual continuity
    val cookieShape = remember { RoundedPolygonShape(MaterialShapes.Cookie4Sided) }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.medium
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // THE DATA
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-2).sp,
                    ),
                    color = MaterialTheme.colorScheme.tertiary
                )

                // VISUAL ICON: A small version of the "Perfect" shape
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = cookieShape
                        )
                        .align(Alignment.CenterVertically),
                    contentAlignment = Alignment.Center
                ) {}
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(
                    if (count == 1) R.string.perfect_day_label else R.string.perfect_days_label
                ),
                style = MaterialTheme.typography.titleMedium.copy(
                    lineHeight = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Footer pushed to the bottom by SpaceBetween — mirrors GlobalCompletionCard
        Text(
            text = stringResource(R.string.habit_perfect_days_footer),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun BentoRowPreview() {
    MaterialTheme {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GlobalCompletionCard(
                percentage = 68,
                modifier = Modifier.weight(1f)
            )
            PerfectDaysCard(
                count = 3,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
