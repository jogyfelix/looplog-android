package com.jolabs.looplog.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.WeekFields
import java.util.Calendar
import java.util.Locale

object DateUtils {
    fun todayEpochDay(): Long = LocalDate.now(ZoneId.systemDefault()).toEpochDay()

    /**
     * The 7 days of the week ordered by the locale's convention (e.g. Sunday-first for
     * en-US/en-IN, Monday-first for most of Europe). Single source of truth for every
     * week-ordered UI so all screens agree with the calendar.
     */
    fun orderedDaysOfWeek(locale: Locale): List<DayOfWeek> {
        val firstDay = WeekFields.of(locale).firstDayOfWeek
        return List(7) { firstDay.plus(it.toLong()) }
    }

    fun formatEpochDay(epochDay: Long): String {
        val date = LocalDate.ofEpochDay(epochDay)
        return date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
    }

    fun Long.toLocalDateFromEpochDays(): LocalDate =
        LocalDate.ofEpochDay(this)

    fun Long.toLocalDateFromEpochMillis(): LocalDate =
        Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

     fun formatHabitTime(timeOfDayMillis: Long?, is24Hour: Boolean): String? {
        return timeOfDayMillis?.let {
            val localTime = Instant.ofEpochMilli(it)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
            val pattern = if (is24Hour) "HH:mm" else "h:mm a"
            localTime.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
        }
    }
}