package com.ballooner.ui.project

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ballooner.data.balloon.FakeBalloonRepository
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
    fun `updates the type of the selected balloon`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            viewModel.addBalloon(BalloonType.SPEAK)
            advanceUntilIdle()
            viewModel.setType(BalloonType.YELL)
            advanceUntilIdle()

            assertEquals(BalloonType.YELL, expectMostRecentItem().selectedBalloon?.type)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `updates the tail length of the selected balloon`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            viewModel.addBalloon(BalloonType.SPEAK)
            advanceUntilIdle()
            viewModel.setTailLength(0.3f)
            advanceUntilIdle()

            assertEquals(0.3f, expectMostRecentItem().selectedBalloon?.tailLength)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `clamps the tail length to the allowed range`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            viewModel.addBalloon(BalloonType.SPEAK)
            advanceUntilIdle()
            viewModel.setTailLength(5f)
            advanceUntilIdle()

            assertEquals(0.4f, expectMostRecentItem().selectedBalloon?.tailLength)
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
