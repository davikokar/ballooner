package com.ballooner.ui.project

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ballooner.data.balloon.FakeBalloonRepository
import com.ballooner.data.image.FakeImageStore
import com.ballooner.data.project.FakeProjectRepository
import com.ballooner.data.settings.FakeSettingsRepository
import com.ballooner.domain.model.AppSettings
import com.ballooner.domain.model.BalloonFont
import com.ballooner.domain.model.BalloonType
import com.ballooner.domain.model.Project
import com.ballooner.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
            settingsRepository = FakeSettingsRepository(),
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
            settingsRepository = FakeSettingsRepository(),
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
    fun `adds a caption with no tail and square corners`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            viewModel.addBalloon(BalloonType.CAPTION)
            advanceUntilIdle()

            val balloon = expectMostRecentItem().balloons.single()
            assertEquals(0f, balloon.tailLength, 0.0001f)
            assertEquals(0f, balloon.cornerRoundness, 0.0001f)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `deleteProject removes the project, cleans up its image, and reports completion`() = runTest {
        val projectRepository = FakeProjectRepository(
            initial = listOf(
                Project(id = 1, name = "Comic", description = "", createdAt = 1, imageUri = "file:///img.jpg"),
            ),
        )
        val imageStore = FakeImageStore()
        val viewModel = ProjectViewModel(
            savedStateHandle = SavedStateHandle(mapOf("projectId" to 1L)),
            projectRepository = projectRepository,
            balloonRepository = FakeBalloonRepository(),
            imageStore = imageStore,
            settingsRepository = FakeSettingsRepository(),
        )
        var deleted = false

        viewModel.uiState.test {
            // Stay subscribed until the project's image is loaded into state.
            while (awaitItem().imageUri == null) { /* await image */ }

            viewModel.deleteProject { deleted = true }
            advanceUntilIdle()
            cancelAndConsumeRemainingEvents()
        }

        assertNull(projectRepository.observeProject(1).first())
        assertEquals(listOf("file:///img.jpg"), imageStore.deleted)
        assertTrue(deleted)
    }

    @Test
    fun `new balloons use the default font from settings`() = runTest {
        val viewModel = ProjectViewModel(
            savedStateHandle = SavedStateHandle(mapOf("projectId" to 1L)),
            projectRepository = FakeProjectRepository(
                initial = listOf(Project(id = 1, name = "Comic", description = "", createdAt = 1)),
            ),
            balloonRepository = FakeBalloonRepository(),
            imageStore = FakeImageStore(),
            settingsRepository = FakeSettingsRepository(AppSettings(defaultFont = BalloonFont.COMIC_SANS_MS)),
        )

        viewModel.uiState.test {
            viewModel.addBalloon(BalloonType.SPEAK)
            advanceUntilIdle()

            assertEquals(BalloonFont.COMIC_SANS_MS, expectMostRecentItem().balloons.single().font)
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
