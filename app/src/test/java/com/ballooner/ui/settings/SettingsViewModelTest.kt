package com.ballooner.ui.settings

import app.cash.turbine.test
import com.ballooner.data.settings.FakeSettingsRepository
import com.ballooner.domain.model.BalloonFont
import com.ballooner.domain.model.TextSizeMode
import com.ballooner.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `emits the stored settings`() = runTest {
        val viewModel = SettingsViewModel(FakeSettingsRepository())

        viewModel.uiState.test {
            val settings = expectMostRecentItem().settings
            assertEquals(BalloonFont.ANIME_ACE, settings.defaultFont)
            assertEquals(true, settings.hideFontSelector)
            assertEquals(TextSizeMode.AUTO, settings.textSizeMode)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `default font choices exclude the generic default font`() {
        assertEquals(false, selectableDefaultFonts.contains(BalloonFont.DEFAULT))
        assertEquals(true, selectableDefaultFonts.contains(BalloonFont.ANIME_ACE))
    }

    @Test
    fun `updates persist to the repository and re-emit`() = runTest {
        val viewModel = SettingsViewModel(FakeSettingsRepository())

        viewModel.uiState.test {
            viewModel.setDefaultFont(BalloonFont.SERIF)
            viewModel.setHideFontSelector(true)
            viewModel.setTextSizeMode(TextSizeMode.AUTO)
            advanceUntilIdle()

            val settings = expectMostRecentItem().settings
            assertEquals(BalloonFont.SERIF, settings.defaultFont)
            assertEquals(true, settings.hideFontSelector)
            assertEquals(TextSizeMode.AUTO, settings.textSizeMode)
            cancelAndConsumeRemainingEvents()
        }
    }
}
