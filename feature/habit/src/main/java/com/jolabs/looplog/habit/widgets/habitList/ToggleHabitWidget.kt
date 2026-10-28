package com.jolabs.looplog.habit.widgets.habitList

import android.content.Context
import android.os.Build
import dagger.hilt.android.EntryPointAccessors
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.action.actionParametersOf
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.jolabs.looplog.design_system.ui.theme.MyWidgetColorScheme
import com.jolabs.looplog.habit.R
import com.jolabs.looplog.model.HabitStatus
import com.jolabs.looplog.util.DateUtils.todayEpochDay
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.lazy.items
import androidx.glance.currentState
import androidx.glance.layout.Box
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.jolabs.looplog.habit.widgets.WidgetEntryPoint
import java.time.LocalDate
import android.content.Intent
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.net.toUri
import androidx.glance.appwidget.cornerRadius


class ToggleHabitWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition
    val isLoadingKey = booleanPreferencesKey("is_loading")
    private val lastUpdateDayKey = longPreferencesKey("last_update_epoch_day")

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {
        // Preload state once if empty, before entering composition (per Glance docs)
        val prefsSnapshot = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val existingJson = prefsSnapshot[stringPreferencesKey("habits_json")] ?: ""
        val currentEpochDay = todayEpochDay()
        val isStale = prefsSnapshot[lastUpdateDayKey] != currentEpochDay

        if (existingJson.isBlank() || isStale) {

            updateAppWidgetState(context, id) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[isLoadingKey] = true
                }
            }

            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    WidgetEntryPoint::class.java
                )
                val repo = entryPoint.habitRepository()
                val today = LocalDate.now()
                val list = repo.getHabitByDateOnce(today.dayOfWeek, todayEpochDay())
                val json = encodeHabits(list.toDto())

                updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[stringPreferencesKey("habits_json")] = json
                        this[lastUpdateDayKey] = currentEpochDay
                        this[isLoadingKey] = false
                    }
                }
            } catch (_: Exception) {
                updateAppWidgetState(context, id) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[isLoadingKey] = false
                    }
                }
            }
        }
        val todayString = context.getString(R.string.today)
        val refreshString = context.getString(R.string.refresh)
        val noHabitString = context.getString(R.string.no_habits_today_short)

        provideContent {
            val prefs = currentState<Preferences>()
            val json = prefs[stringPreferencesKey("habits_json")] ?: "[]"
            val isLoadingState = prefs[isLoadingKey] ?: false

            GlanceTheme(
                colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    GlanceTheme.colors
                } else {
                    MyWidgetColorScheme.colors
                }
            ) {
                if (isLoadingState) {
                    Box(
                        modifier = GlanceModifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (json.isBlank()) {
                    Box(
                        modifier = GlanceModifier.fillMaxSize()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            noHabitString,
                            style = TextStyle(
                                color = GlanceTheme.colors.outline
                            )
                        )
                    }
                } else {
                    val habits = decodeHabits(json).toDomain()

                    Column(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .cornerRadius(8.dp)
                            .background( GlanceTheme.colors.background)
                            .padding(8.dp)
                        , verticalAlignment = Alignment.Top
                    ) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Text(
                                todayString, style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = GlanceTheme.colors.onBackground,
                                ),
                                modifier = GlanceModifier.clickable(
                                    actionStartActivity(
                                        Intent(Intent.ACTION_VIEW, "looplog://habit/".toUri()).apply {
                                            setPackage(context.packageName)
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        }
                                    )
                                )
                            )

                            Spacer(GlanceModifier.defaultWeight())
                            Image(
                                provider = ImageProvider(R.drawable.refresh),
                                contentDescription = refreshString,
                                colorFilter = ColorFilter.tint(GlanceTheme.colors.onBackground),
                                modifier = GlanceModifier
                                    .size(20.dp)
                                    .clickable(actionRunCallback<RefreshHabitAction>())
                            )
                        }

                        Spacer(GlanceModifier.height(8.dp))

                        if (habits.isEmpty()) {
                            Box(
                                modifier = GlanceModifier.fillMaxSize()
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    noHabitString,
                                    style = TextStyle(
                                        color = GlanceTheme.colors.outline
                                    )
                                )
                            }
                        } else {
                            LazyColumn {
                                items(habits, itemId = { it.id }) { item ->
                                    Row(
                                        modifier = GlanceModifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable(
                                                actionRunCallback<OpenToggleHabitAction>(
                                                    actionParametersOf(
                                                        HabitIdKey to item.id,
                                                        EpochDateKey to todayEpochDay(),
                                                        StatusKey to item.habitState.name,
                                                    )
                                                )
                                            ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // State icon based on habit status
                                        val iconRes = when (item.habitState) {
                                            HabitStatus.COMPLETED -> R.drawable.check
                                            HabitStatus.SKIPPED -> R.drawable.remove
                                            HabitStatus.NONE -> R.drawable.circle_outline
                                        }

                                        val iconColor = when (item.habitState) {
                                            HabitStatus.COMPLETED -> GlanceTheme.colors.primary
                                            HabitStatus.SKIPPED, HabitStatus.NONE -> GlanceTheme.colors.outline
                                        }

                                        Image(
                                            provider = ImageProvider(iconRes),
                                            contentDescription = item.habitState.name,
                                            colorFilter = ColorFilter.tint(iconColor),
                                            modifier = GlanceModifier.size(24.dp)
                                        )

                                        Spacer(GlanceModifier.size(12.dp))

                                        Text(
                                            text = item.name,
                                            style = TextStyle(
                                                color = GlanceTheme.colors.onBackground,
                                                fontSize = 14.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
