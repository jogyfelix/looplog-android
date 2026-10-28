package com.jolabs.looplog.navigation

import androidx.compose.animation.AnimatedContentTransitionScope

internal const val NAVIGATION_ANIMATION_DURATION_MILLIS = 500

// Compose names the direction the content travels: forward moves left, while Back reverses right.
internal fun navigationSlideDirection(
    isPop: Boolean,
): AnimatedContentTransitionScope.SlideDirection = if (isPop) {
    AnimatedContentTransitionScope.SlideDirection.Right
} else {
    AnimatedContentTransitionScope.SlideDirection.Left
}
