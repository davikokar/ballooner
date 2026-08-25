package com.ballooner.ui.projectcreate

import app.cash.turbine.test
import com.ballooner.data.project.FakeProjectRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import com.ballooner.util.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectCreateViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `canSave is false when the name is blank`() {
        val viewModel = ProjectCreateViewModel(FakeProjectRepository())

        viewModel.onNameChange("   ")

        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun `canSave is true once a name is entered`() {
        val viewModel = ProjectCreateViewModel(FakeProjectRepository())

        viewModel.onNameChange("Space Cats")

        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `save persists the project and marks the state as saved`() = runTest {
        val repository = FakeProjectRepository()
        val viewModel = ProjectCreateViewModel(repository)
        viewModel.onNameChange("Space Cats")
        viewModel.onDescriptionChange("A feline space opera")

        viewModel.save()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSaved)
        repository.observeProjects().test {
            assertEquals("Space Cats", awaitItem().single().name)
            cancelAndConsumeRemainingEvents()
        }
    }
}
