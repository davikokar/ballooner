package com.ballooner.ui.project

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ballooner.data.balloon.FakeBalloonRepository
import com.ballooner.data.image.ComposedImage
import com.ballooner.data.image.FakeImageStore
import com.ballooner.data.panel.FakePanelRepository
import com.ballooner.data.project.FakeProjectRepository
import com.ballooner.data.settings.FakeSettingsRepository
import com.ballooner.domain.model.AppSettings
import com.ballooner.domain.model.BalloonFont
import com.ballooner.domain.model.BalloonType
import com.ballooner.domain.model.ImagePlacement
import com.ballooner.domain.model.ImagePosition
import com.ballooner.domain.model.Project
import com.ballooner.domain.model.RectFraction
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
            panelRepository = FakePanelRepository(),
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
    fun `isProcessingImage is false again once picking an image finishes`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            viewModel.onImagePicked("content://image/1")
            advanceUntilIdle()

            assertEquals(false, expectMostRecentItem().isProcessingImage)
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
            panelRepository = FakePanelRepository(),
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
    fun `adding an image composes it with the existing one and switches to the merged uri`() = runTest {
        val imageStore = FakeImageStore().apply {
            composeResult = ComposedImage(
                uri = "merged-uri",
                previousImageRect = RectFraction(left = 0.5f, top = 0f, width = 0.5f, height = 1f),
                newImageRect = RectFraction(left = 0f, top = 0f, width = 0.5f, height = 1f),
            )
        }
        val projectRepository = FakeProjectRepository(
            initial = listOf(
                Project(id = 1, name = "Comic", description = "", createdAt = 1, imageUri = "existing-uri"),
            ),
        )
        val viewModel = ProjectViewModel(
            savedStateHandle = SavedStateHandle(mapOf("projectId" to 1L)),
            projectRepository = projectRepository,
            balloonRepository = FakeBalloonRepository(),
            panelRepository = FakePanelRepository(),
            imageStore = imageStore,
            settingsRepository = FakeSettingsRepository(),
        )

        viewModel.uiState.test {
            while (awaitItem().imageUri == null) { /* await the initial project image */ }
            val placement = ImagePlacement(RectFraction(0f, 0f, 1f, 1f), ImagePosition.RIGHT)

            viewModel.onAddImage("added-uri", placement)
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals("merged-uri", state.imageUri)
            assertEquals(false, state.isProcessingImage)
            assertEquals(listOf("existing-uri", "added-uri", placement), imageStore.lastComposeRequest)
            assertEquals(listOf("existing-uri"), imageStore.deleted)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `adding an image remaps existing balloons into the old image's new, smaller rect`() = runTest {
        val imageStore = FakeImageStore().apply {
            composeResult = ComposedImage(
                uri = "merged-uri",
                previousImageRect = RectFraction(left = 0.5f, top = 0f, width = 0.5f, height = 1f),
                newImageRect = RectFraction(left = 0f, top = 0f, width = 0.5f, height = 1f),
            )
        }
        val projectRepository = FakeProjectRepository(
            initial = listOf(
                Project(id = 1, name = "Comic", description = "", createdAt = 1, imageUri = "existing-uri"),
            ),
        )
        val viewModel = ProjectViewModel(
            savedStateHandle = SavedStateHandle(mapOf("projectId" to 1L)),
            projectRepository = projectRepository,
            balloonRepository = FakeBalloonRepository(),
            panelRepository = FakePanelRepository(),
            imageStore = imageStore,
            settingsRepository = FakeSettingsRepository(),
        )

        viewModel.uiState.test {
            while (awaitItem().imageUri == null) { /* await the initial project image */ }

            viewModel.addBalloon(BalloonType.SPEAK)
            advanceUntilIdle()
            expectMostRecentItem()

            viewModel.onAddImage(
                "added-uri",
                ImagePlacement(RectFraction(0f, 0f, 1f, 1f), ImagePosition.RIGHT),
            )
            advanceUntilIdle()

            val balloon = expectMostRecentItem().balloons.single()
            assertEquals(0.75f, balloon.centerX, 0.0001f)
            assertEquals(0.2f, balloon.width, 0.0001f)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `deleting an edge image crops the canvas and expands the remaining panel`() = runTest {
        val imageStore = FakeImageStore().apply { removeResult = "cropped-uri" }
        val panelRepository = FakePanelRepository()
        val remainingPanel = RectFraction(left = 0f, top = 0f, width = 0.5f, height = 1f)
        val panelToDelete = RectFraction(left = 0.5f, top = 0f, width = 0.5f, height = 1f)
        val projectRepository = FakeProjectRepository(
            initial = listOf(
                Project(id = 1, name = "Comic", description = "", createdAt = 1, imageUri = "existing-uri"),
            ),
        )
        val viewModel = ProjectViewModel(
            savedStateHandle = SavedStateHandle(mapOf("projectId" to 1L)),
            projectRepository = projectRepository,
            balloonRepository = FakeBalloonRepository(),
            panelRepository = panelRepository,
            imageStore = imageStore,
            settingsRepository = FakeSettingsRepository(),
        )
        panelRepository.replacePanels(1L, listOf(remainingPanel, panelToDelete))

        viewModel.uiState.test {
            while (awaitItem().panels.size < 2) { /* await the seeded panels */ }

            viewModel.onDeleteImage(panelToDelete)
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals("cropped-uri", state.imageUri)
            assertEquals(listOf(RectFraction(0f, 0f, 1f, 1f)), state.panels)
            assertEquals(Triple("existing-uri", panelToDelete, remainingPanel), imageStore.lastRemoveRequest)
            assertEquals(listOf("existing-uri"), imageStore.deleted)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `deleting an image panel removes balloons centered on it, but not others`() = runTest {
        val imageStore = FakeImageStore().apply { removeResult = "cropped-uri" }
        val panelRepository = FakePanelRepository()
        val balloonRepository = FakeBalloonRepository()
        val panelToDelete = RectFraction(left = 0.5f, top = 0f, width = 0.5f, height = 1f)
        val projectRepository = FakeProjectRepository(
            initial = listOf(
                Project(id = 1, name = "Comic", description = "", createdAt = 1, imageUri = "existing-uri"),
            ),
        )
        val viewModel = ProjectViewModel(
            savedStateHandle = SavedStateHandle(mapOf("projectId" to 1L)),
            projectRepository = projectRepository,
            balloonRepository = balloonRepository,
            panelRepository = panelRepository,
            imageStore = imageStore,
            settingsRepository = FakeSettingsRepository(),
        )
        panelRepository.replacePanels(1L, listOf(RectFraction(0f, 0f, 0.5f, 1f), panelToDelete))

        viewModel.uiState.test {
            while (awaitItem().panels.size < 2) { /* await the seeded panels */ }

            viewModel.selectBalloon(null)
            viewModel.addBalloon(BalloonType.SPEAK) // lands at the default center (0.5, 0.5), on the deleted panel
            advanceUntilIdle()
            val onOtherPanel = expectMostRecentItem().balloons.single().copy(centerX = 0.25f)
            viewModel.commitBalloon(onOtherPanel)
            advanceUntilIdle()
            expectMostRecentItem()

            viewModel.addBalloon(BalloonType.SPEAK) // default center (0.5, 0.5) is on the deleted panel
            advanceUntilIdle()
            expectMostRecentItem()

            viewModel.onDeleteImage(panelToDelete)
            advanceUntilIdle()

            val remaining = expectMostRecentItem().balloons.single()
            assertEquals(onOtherPanel.id, remaining.id)
            assertEquals(0.5f, remaining.centerX, 0.0001f)
            assertEquals(onOtherPanel.width * 2f, remaining.width, 0.0001f)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `does not delete the only panel, since that's the whole comic`() = runTest {
        val imageStore = FakeImageStore().apply { removeResult = "cropped-uri" }
        val panelRepository = FakePanelRepository()
        val onlyPanel = RectFraction(0f, 0f, 1f, 1f)
        val projectRepository = FakeProjectRepository(
            initial = listOf(
                Project(id = 1, name = "Comic", description = "", createdAt = 1, imageUri = "existing-uri"),
            ),
        )
        val viewModel = ProjectViewModel(
            savedStateHandle = SavedStateHandle(mapOf("projectId" to 1L)),
            projectRepository = projectRepository,
            balloonRepository = FakeBalloonRepository(),
            panelRepository = panelRepository,
            imageStore = imageStore,
            settingsRepository = FakeSettingsRepository(),
        )
        panelRepository.replacePanels(1L, listOf(onlyPanel))

        viewModel.uiState.test {
            while (awaitItem().panels.isEmpty()) { /* await the seeded panel */ }
            cancelAndConsumeRemainingEvents()
        }

        // A no-op produces no new state emission, so assert on the current value directly
        // instead of awaiting one (StateFlow dedupes equal consecutive values).
        viewModel.onDeleteImage(onlyPanel)
        advanceUntilIdle()

        assertEquals("existing-uri", viewModel.uiState.value.imageUri)
        assertNull(imageStore.lastRemoveRequest)
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
            panelRepository = FakePanelRepository(),
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
            panelRepository = FakePanelRepository(),
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
