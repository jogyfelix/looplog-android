package com.jolabs.looplog.settings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
object AppearanceRoute

fun NavGraphBuilder.settingsNavGraph(onNavigateUp: () -> Unit) {
    composable<AppearanceRoute> {
        AppearanceScreenRoute(onNavigateUp = onNavigateUp)
    }
}
