package com.jolabs.looplog.habit.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DateDisplayComponent(
    selectedDate: Long,
    modifier: Modifier = Modifier
) {
    val localDate = LocalDate.ofEpochDay(selectedDate)
    val locale = LocalConfiguration.current.locales[0]
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        // Day of week (e.g., "Thursday")
        Text(
            text = localDate.dayOfWeek.getDisplayName(TextStyle.FULL, locale),
            style = MaterialTheme.typography.titleMedium,
            fontSize = 24.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        // Date number (e.g., "25")
        Text(
            text = localDate.dayOfMonth.toString(),
            style = MaterialTheme.typography.headlineLarge,
            fontSize = 74.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        // Month name (e.g., "January")
        Text(
            text = localDate.month.getDisplayName(TextStyle.FULL, locale).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            fontSize = 48.sp,
            fontWeight = FontWeight.ExtraLight,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DateDisplayComponentPreview() {
    MaterialTheme {
        DateDisplayComponent(
            selectedDate = LocalDate.now().toEpochDay(),
            modifier = Modifier.padding(16.dp)
        )
    }
}
