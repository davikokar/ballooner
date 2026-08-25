package com.ballooner.ui.projectlist

import app.cash.turbine.test
import com.ballooner.data.project.FakeProjectRepository
import com.ballooner.domain.model.Project
import com.ballooner.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `emits Empty when there are no projects`() = runTest {
        val viewModel = ProjectListViewModel(FakeProjectRepository())

        viewModel.uiState.test {
            var state = awaitItem()
            if (state is ProjectListUiState.Loading) state = awaitItem()

            assertEquals(ProjectListUiState.Empty, state)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `emits Content with the stored projects`() = runTest {
        val repository = FakeProjectRepository(
            initial = listOf(Project(id = 1, name = "Space Cats", description = "", createdAt = 1)),
        )
        val viewModel = ProjectListViewModel(repository)

        viewModel.uiState.test {
            var state = awaitItem()
            if (state is ProjectListUiState.Loading) state = awaitItem()

            assertTrue(state is ProjectListUiState.Content)
            assertEquals("Space Cats", (state as ProjectListUiState.Content).projects.single().name)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `deleteProject removes the project from the emitted state`() = runTest {
        val repository = FakeProjectRepository(
            initial = listOf(Project(id = 1, name = "Space Cats", description = "", createdAt = 1)),
        )
        val viewModel = ProjectListViewModel(repository)

        viewModel.uiState.test {
            var state = awaitItem()
            if (state is ProjectListUiState.Loading) state = awaitItem()
            assertTrue(state is ProjectListUiState.Content)

            viewModel.deleteProject(1)

            assertEquals(ProjectListUiState.Empty, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }
}
