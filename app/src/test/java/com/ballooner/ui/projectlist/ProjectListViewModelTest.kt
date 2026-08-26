package com.ballooner.ui.projectlist

import app.cash.turbine.test
import com.ballooner.data.image.FakeImageStore
import com.ballooner.data.project.FakeProjectRepository
import com.ballooner.domain.model.Project
import com.ballooner.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(
        repository: FakeProjectRepository,
        imageStore: FakeImageStore = FakeImageStore(),
    ) = ProjectListViewModel(repository, imageStore)

    @Test
    fun `emits Empty when there are no projects`() = runTest {
        val viewModel = viewModel(FakeProjectRepository())

        viewModel.uiState.test {
            var state = awaitItem()
            if (state is ProjectListUiState.Loading) state = awaitItem()

            assertEquals(ProjectListUiState.Empty, state)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `emits Content with the stored projects`() = runTest {
        val viewModel = viewModel(
            FakeProjectRepository(
                initial = listOf(Project(id = 1, name = "Space Cats", description = "", createdAt = 1)),
            ),
        )

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
        val viewModel = viewModel(
            FakeProjectRepository(
                initial = listOf(Project(id = 1, name = "Space Cats", description = "", createdAt = 1)),
            ),
        )

        viewModel.uiState.test {
            var state = awaitItem()
            if (state is ProjectListUiState.Loading) state = awaitItem()
            assertTrue(state is ProjectListUiState.Content)

            viewModel.deleteProject(1)

            assertEquals(ProjectListUiState.Empty, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `deleteProject deletes the project's image copy`() = runTest {
        val imageStore = FakeImageStore()
        val viewModel = viewModel(
            FakeProjectRepository(
                initial = listOf(
                    Project(id = 1, name = "Space Cats", description = "", createdAt = 1, imageUri = "file:///img.jpg"),
                ),
            ),
            imageStore,
        )

        viewModel.deleteProject(1)
        advanceUntilIdle()

        assertEquals(listOf("file:///img.jpg"), imageStore.deleted)
    }

    @Test
    fun `createProject adds a default titled project and reports its id`() = runTest {
        val repository = FakeProjectRepository()
        val viewModel = viewModel(repository)
        var createdId = -1L

        viewModel.createProject { createdId = it }
        advanceUntilIdle()

        assertTrue(createdId > 0)
        repository.observeProjects().test {
            assertEquals("My Comic 1", awaitItem().single().name)
            cancelAndConsumeRemainingEvents()
        }
    }
}
