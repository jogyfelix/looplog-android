package com.jolabs.looplog.appfunctions

import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import com.jolabs.looplog.habit.appfunctions.HabitAppFunctions
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main entry point for AppFunctions in the HabitApp.
 * This class is annotated with @AppFunctionServiceEntryPoint which triggers
 * the KSP compiler to generate the concrete implementation and metadata.
 */
@RequiresApi(36)
@AndroidEntryPoint
@AppFunctionServiceEntryPoint(
    serviceName = "HabitAppFunctionsService",
    appFunctionXmlFileName = "habit_app_functions"
)
abstract class HabitAppFunctionService : AppFunctionService() {

    @Inject
    lateinit var habitAppFunctions: HabitAppFunctions

    /**
     * Lists all habits currently configured in the app.
     *
     * @param context The execution context.
     * @return A list of habit names and their repeat days, or an empty message if no habits exist.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun listHabits(context: AppFunctionContext): String {
        return habitAppFunctions.listHabits(applicationContext)
    }

    /**
     * Creates a new habit with the specified details.
     *
     * @param context The execution context.
     * @param name The name of the habit.
     * @param daysOfWeek The days of the week the habit should repeat (for example, MONDAY, TUESDAY).
     * @param description Optional description of the habit.
     * @param alarmTimeMillis Optional time of day for the reminder alarm in milliseconds from start of day.
     * @return A success message, or an error message if creation failed.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun createHabit(
        context: AppFunctionContext,
        name: String,
        daysOfWeek: List<String>,
        description: String? = null,
        alarmTimeMillis: Long? = null
    ): String {
        return habitAppFunctions.createHabit(
            applicationContext,
            name,
            daysOfWeek,
            description,
            alarmTimeMillis
        )
    }

    /**
     * Logs the completion state of a habit for today.
     *
     * @param context The execution context.
     * @param name The name of the habit to log.
     * @param state The state to log (one of NONE, COMPLETED, or SKIPPED).
     * @return A success message, or an error message if the habit was not found or logging failed.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun logHabitForToday(
        context: AppFunctionContext,
        name: String,
        state: String
    ): String {
        return habitAppFunctions.logHabitForToday(applicationContext, name, state)
    }

    /**
     * Logs the completion state for ALL habits scheduled for today.
     *
     * @param context The execution context.
     * @param state The state to log (one of NONE, COMPLETED, or SKIPPED).
     * @return A success message indicating how many habits were updated.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun logAllHabitsForToday(
        context: AppFunctionContext,
        state: String
    ): String {
        return habitAppFunctions.logAllHabitsForToday(applicationContext, state)
    }
}
