package com.jolabs.looplog.changelog

import com.google.common.truth.Truth.assertThat
import com.jolabs.looplog.data.repository.ChangelogRepository
import com.jolabs.looplog.model.changelog.Changelog
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChangelogViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository: ChangelogRepository = mockk()
    private val changelog = Changelog(
        versionCode = 80,
        versionName = "1.2.31",
        title = "What's new",
        notes = listOf("Added weekly goals"),
        detailsUrl = "https://feedback.example.com/updates",
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `automatic load marks eligible changelog only after it is presented`() = runTest(dispatcher) {
        coEvery { repository.hasPendingAutomaticChangelog() } returns true
        coEvery { repository.getCurrentChangelog() } returns changelog
        coEvery { repository.shouldShowAutomatically(changelog) } returns true
        coEvery { repository.markShown(80) } returns Unit
        val viewModel = ChangelogViewModel(repository)

        viewModel.loadAutomatically()
        dispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.state.value).isEqualTo(ChangelogUiState.Visible(changelog))
        coVerify(exactly = 0) { repository.markShown(any()) }

        viewModel.onPresented(changelog)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.markShown(80) }
    }

    @Test
    fun `automatic load stays hidden when changelog is not eligible`() = runTest(dispatcher) {
        coEvery { repository.hasPendingAutomaticChangelog() } returns true
        coEvery { repository.getCurrentChangelog() } returns changelog
        coEvery { repository.shouldShowAutomatically(changelog) } returns false
        val viewModel = ChangelogViewModel(repository)

        viewModel.loadAutomatically()
        dispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.state.value).isEqualTo(ChangelogUiState.Hidden)
        coVerify(exactly = 0) { repository.markShown(any()) }
    }

    @Test
    fun `automatic load avoids remote lookup when there is no pending update`() = runTest(dispatcher) {
        coEvery { repository.hasPendingAutomaticChangelog() } returns false
        val viewModel = ChangelogViewModel(repository)

        viewModel.loadAutomatically()
        dispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.state.value).isEqualTo(ChangelogUiState.Hidden)
        coVerify(exactly = 0) { repository.getCurrentChangelog() }
    }

    @Test
    fun `manual open presents current changelog even after automatic attempt`() = runTest(dispatcher) {
        coEvery { repository.getCurrentChangelog() } returns changelog
        coEvery { repository.shouldShowAutomatically(changelog) } returns false
        coEvery { repository.markShown(80) } returns Unit
        val viewModel = ChangelogViewModel(repository)
        viewModel.loadAutomatically()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.openManually()
        dispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.state.value).isEqualTo(ChangelogUiState.Visible(changelog))
        viewModel.onPresented(changelog)
        dispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 1) { repository.markShown(80) }
    }

    @Test
    fun `manual open reports unavailable when current notes cannot be loaded`() = runTest(dispatcher) {
        coEvery { repository.getCurrentChangelog() } returns null
        val viewModel = ChangelogViewModel(repository)

        viewModel.openManually()
        dispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.state.value).isEqualTo(ChangelogUiState.Unavailable)
    }

    @Test
    fun `language change reloads a visible changelog translation`() = runTest(dispatcher) {
        val hindiChangelog = changelog.copy(
            title = "नया क्या है",
            notes = listOf("साप्ताहिक लक्ष्य जोड़े गए"),
        )
        coEvery { repository.getCurrentChangelog() } returnsMany listOf(changelog, hindiChangelog)
        val viewModel = ChangelogViewModel(repository)
        viewModel.onLanguageChanged(listOf("en-US"))
        viewModel.openManually()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onLanguageChanged(listOf("hi-IN"))
        dispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.state.value).isEqualTo(ChangelogUiState.Visible(hindiChangelog))
        coVerify(exactly = 2) { repository.getCurrentChangelog() }
    }

    @Test
    fun `language change keeps a dismissed changelog hidden`() = runTest(dispatcher) {
        coEvery { repository.getCurrentChangelog() } returns changelog
        val viewModel = ChangelogViewModel(repository)
        viewModel.onLanguageChanged(listOf("en-US"))
        viewModel.openManually()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.dismiss()

        viewModel.onLanguageChanged(listOf("hi-IN"))
        dispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.state.value).isEqualTo(ChangelogUiState.Hidden)
        coVerify(exactly = 1) { repository.getCurrentChangelog() }
    }

    @Test
    fun `failed translation refresh keeps the visible changelog`() = runTest(dispatcher) {
        coEvery { repository.getCurrentChangelog() } returnsMany listOf(changelog, null)
        val viewModel = ChangelogViewModel(repository)
        viewModel.onLanguageChanged(listOf("en-US"))
        viewModel.openManually()
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onLanguageChanged(listOf("hi-IN"))
        dispatcher.scheduler.advanceUntilIdle()

        assertThat(viewModel.state.value).isEqualTo(ChangelogUiState.Visible(changelog))
    }
}
