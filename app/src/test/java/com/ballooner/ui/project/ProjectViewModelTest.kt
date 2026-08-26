package com.ballooner.ui.project

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ballooner.data.balloon.FakeBalloonRepository
import com.ballooner.data.image.FakeImageStore
import com.ballooner.data.project.FakeProjectRepository
import com.ballooner.domain.model.BalloonType
import com.ballooner.domain.model.Project
import com.ballooner.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(): ProjectViewModel {
        val projectRepository = FakeProjectRepository(
            initial = listOf(Project(id = 1, name = "Comic", description = "", createdAt = 1)),
        )
        return ProjectViewModel(
            savedStateHandle = SavedStateHandle(mapOf("projectId" to 1L)),
            projectRepository = projectRepository,
            balloonRepository = FakeBalloonRepository(),
            imageStore = FakeImageStore(),
        )
    }

    @Test
    fun `stores the picked image uri`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            viewModel.onImagePicked("content://image/1")
            advanceUntilIdle()

            assertEquals("content://image/1", expectMostRecentItem().imageUri)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `replacing the image deletes the previous copy`() = runTest {
        val imageStore = FakeImageStore()
        val viewModel = ProjectViewModel(
            savedStateHandle = SavedStateHandle(mapOf("projectId" to 1L)),
            projectRepository = FakeProjectRepository(
                initial = listOf(Project(id = 1, name = "Comic", description = "", createdAt = 1)),
            ),
            balloonRepository = FakeBalloonRepository(),
            imageStore = imageStore,
        )

        viewModel.uiState.test {
            viewModel.onImagePicked("uri1")
            advanceUntilIdle()
            viewModel.onImagePicked("uri2")
            advanceUntilIdle()

            assertEquals("uri2", expectMostRecentItem().imageUri)
            assertEquals(listOf("uri1"), imageStore.deleted)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `adds a balloon of the requested type and selects it`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            viewModel.addBalloon(BalloonType.THINK)
            advanceUntilIdle()

            val state = expectMostRecentItem()
            val balloon = state.balloons.single()
            assertEquals(BalloonType.THINK, balloon.type)
            assertEquals(balloon.id, state.selectedBalloonId)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `updates the text of a balloon`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            viewModel.addBalloon(BalloonType.SPEAK)
            advanceUntilIdle()
            val balloon = expectMostRecentItem().balloons.single()

            viewModel.commitBalloon(balloon.copy(text = "Kapow!"))
            advanceUntilIdle()

            assertEquals("Kapow!", expectMostRecentItem().balloons.single().text)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `commitBalloon clamps size and tail length to the allowed range`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            viewModel.addBalloon(BalloonType.SPEAK)
            advanceUntilIdle()
            val balloon = expectMostRecentItem().balloons.single()

            viewModel.commitBalloon(
                balloon.copy(
                    width = 5f,
                    height = 5f,
                    tailLength = 5f,
                    cornerRoundness = 5f,
                    tailWidth = 9f,
                    fontSize = 99f,
                ),
            )
            advanceUntilIdle()

            val saved = expectMostRecentItem().balloons.single()
            assertEquals(1f, saved.width)
            assertEquals(1f, saved.height)
            assertEquals(0.4f, saved.tailLength)
            assertEquals(1f, saved.cornerRoundness)
            assertEquals(1.5f, saved.tailWidth)
            assertEquals(48f, saved.fontSize)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `deletes the selected balloon`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            viewModel.addBalloon(BalloonType.SPEAK)
            advanceUntilIdle()
            viewModel.deleteSelectedBalloon()
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertTrue(state.balloons.isEmpty())
            assertNull(state.selectedBalloonId)
            cancelAndConsumeRemainingEvents()
        }
    }
}
