package com.jolabs.looplog.navigation

import Resource
import com.google.common.truth.Truth.assertThat
import com.jolabs.looplog.data.repository.ai.AiFeatureAvailability
import com.jolabs.looplog.model.HabitBasic
import com.jolabs.looplog.model.HabitStatus
import org.junit.jupiter.api.Test

class AppContentStateTest {

    @Test
    fun `loading after main keeps the main root visible`() {
        val state = AppContentState.Main.reduce(Resource.Loading())

        assertThat(state).isEqualTo(AppContentState.Main)
    }

    @Test
    fun `successful empty result selects the empty root`() {
        val state = AppContentState.Loading.reduce(Resource.Success(emptyList()))

        assertThat(state).isEqualTo(AppContentState.Empty)
    }

    @Test
    fun `successful non-empty result selects the main root`() {
        val habit = HabitBasic(1, "Read", "", "0", "0", null, HabitStatus.NONE)

        val state = AppContentState.Loading.reduce(Resource.Success(listOf(habit)))

        assertThat(state).isEqualTo(AppContentState.Main)
    }

    @Test
    fun `repository error is not treated as an empty database`() {
        val state = AppContentState.Loading.reduce(Resource.Error("Database unavailable"))

        assertThat(state).isEqualTo(AppContentState.Error("Database unavailable"))
    }

    @Test
    fun `repository error after main keeps the main root visible`() {
        val state = AppContentState.Main.reduce(Resource.Error<List<HabitBasic>>("Database unavailable"))

        assertThat(state).isEqualTo(AppContentState.Main)
    }

    @Test
    fun `checking AI availability keeps the empty state card loading`() {
        val state = AiFeatureAvailability.Checking.toEmptyStateCardState()

        assertThat(state).isEqualTo(EmptyStateCardState.Loading)
    }

    @Test
    fun `available AI selects the smart habit card`() {
        val state = AiFeatureAvailability.Available.toEmptyStateCardState()

        assertThat(state).isEqualTo(EmptyStateCardState.Smart)
    }

    @Test
    fun `unavailable AI selects the manual habit card`() {
        val state = AiFeatureAvailability.Unavailable.toEmptyStateCardState()

        assertThat(state).isEqualTo(EmptyStateCardState.Manual)
    }
}
