package com.jolabs.looplog.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class AppNavigationMotionTest {

    @Test
    fun `forward navigation moves screens left`() {
        assertThat(navigationSlideDirection(isPop = false))
            .isEqualTo(AnimatedContentTransitionScope.SlideDirection.Left)
    }

    @Test
    fun `back navigation moves screens right`() {
        assertThat(navigationSlideDirection(isPop = true))
            .isEqualTo(AnimatedContentTransitionScope.SlideDirection.Right)
    }
}
