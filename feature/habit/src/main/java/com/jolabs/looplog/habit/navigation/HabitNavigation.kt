package com.jolabs.looplog.habit.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.jolabs.looplog.habit.ui.habitCreate.CreateHabitRoute
import com.jolabs.looplog.habit.ui.habitDetail.HabitDetailRoute
import com.jolabs.looplog.habit.ui.aiHabitCreate.AiHabitCreateScreen
import kotlinx.serialization.Serializable

@Serializable data class HabitCreateRoute(val habitId: Long?, val fromDetail: Boolean = false)
@Serializable data class HabitDetailRoute(val habitId: Long)
@Serializable object AiHabitCreateRoute

fun NavGraphBuilder.habitNavGraph(onCreatePress : (habitId : Long?, fromDetail: Boolean) -> Unit,
                                   onDetailPress : (habitId : Long) -> Unit,
                                   onAiCreatePress : () -> Unit,
                                   onNavigateUp : () -> Unit){
     composable<HabitCreateRoute>(
           deepLinks = listOf(
                navDeepLink {
                     uriPattern = "looplog://habit/create?habitId={habitId}"
                }
           ),
           ) { backStackEntry ->
          val habitId = backStackEntry.arguments!!.getLong("habitId")
          val fromDetail = backStackEntry.arguments?.getBoolean("fromDetail") ?: false
          CreateHabitRoute(
               onNavigateUp = onNavigateUp,
               habitId = habitId,
               fromDetail = fromDetail
          )
     }

     composable<HabitDetailRoute> { backStackEntry ->
          val habitId = backStackEntry.arguments!!.getLong("habitId")
          HabitDetailRoute(
               habitId = habitId,
               onNavigateUp = onNavigateUp,
               onEditPress = { id -> onCreatePress(id, true) } // fromDetail = true
          )
     }

     composable<AiHabitCreateRoute> {
          AiHabitCreateScreen(
               onClose = onNavigateUp,
               onAddHabits = {
                    // Navigate back after habits are added
                    onNavigateUp()
               }
          )
     }
}
