package com.ballooner.ui.project

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.ballooner.data.balloon.FakeBalloonRepository
import com.ballooner.data.image.ComposedImage
import com.ballooner.data.image.FakeImageStore
import com.ballooner.data.image.InitialImageGrid
import com.ballooner.data.image.RearrangedImage
import com.ballooner.data.panel.FakePanelRepository
import com.ballooner.data.project.FakeProjectRepository
import com.ballooner.data.settings.FakeSettingsRepository
import com.ballooner.domain.model.AppSettings
import com.ballooner.domain.model.Balloon
import com.ballooner.domain.model.BalloonFont
import com.ballooner.domain.model.BalloonType
import com.ballooner.domain.model.ImagePlacement
import com.ballooner.domain.model.ImagePosition
import com.ballooner.domain.model.Project
import com.ballooner.domain.model.RectFraction
import com.ballooner.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
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
    fun `initial image selection creates a grid using the configured columns`() = runTest {
        val panels = listOf(
            RectFraction(0f, 0f, 0.5f, 1f),
            RectFraction(0.5f, 0f, 0.5f, 1f),
        )
        val imageStore = FakeImageStore().apply {
            initialGridResult = InitialImageGrid("grid-uri", panels)
        }
        val viewModel = ProjectViewModel(
            savedStateHandle = SavedStateHandle(mapOf("projectId" to 1L)),
            projectRepository = FakeProjectRepository(
                initial = listOf(Project(id = 1, name = "Comic", description = "", createdAt = 1)),
            ),
            balloonRepository = FakeBalloonRepository(),
            panelRepository = FakePanelRepository(),
            imageStore = imageStore,
            settingsRepository = FakeSettingsRepository(AppSettings(layoutColumns = 4)),
        )

        viewModel.uiState.test {
            viewModel.onInitialImagesPicked(listOf("one", "two"))
            advanceUntilIdle()

            assertEquals(listOf("one", "two") to 4, imageStore.lastInitialGridRequest)
            val state = expectMostRecentItem()
            assertEquals("grid-uri", state.imageUri)
            assertEquals(panels, state.panels)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `initial image selection does nothing after a comic already has an image`() = runTest {
        val imageStore = FakeImageStore()
        val viewModel = ProjectViewModel(
            savedStateHandle = SavedStateHandle(mapOf("projectId" to 1L)),
            projectRepository = FakeProjectRepository(
                initial = listOf(
                    Project(id = 1, name = "Comic", description = "", createdAt = 1, imageUri = "existing"),
                ),
            ),
            balloonRepository = FakeBalloonRepository(),
            panelRepository = FakePanelRepository(),
            imageStore = imageStore,
            settingsRepository = FakeSettingsRepository(),
        )
        viewModel.uiState.test {
            advanceUntilIdle()
            assertEquals("existing", expectMostRecentItem().imageUri)

            viewModel.onInitialImagesPicked(listOf("one", "two"))
            advanceUntilIdle()

            assertNull(imageStore.lastInitialGridRequest)
            assertEquals("existing", viewModel.uiState.value.imageUri)
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
    fun `moving a panel rebuilds the image and persists shifted panel rectangles`() = runTest {
        val first = RectFraction(0f, 0f, 0.48f, 0.48f)
        val second = RectFraction(0.52f, 0f, 0.48f, 0.48f)
        val third = RectFraction(0f, 0.52f, 0.48f, 0.48f)
        val rearrangedRects = listOf(
            RectFraction(0.52f, 0f, 0.48f, 0.48f),
            RectFraction(0f, 0.52f, 0.48f, 0.48f),
            RectFraction(0f, 0f, 0.48f, 0.48f),
        )
        val imageStore = FakeImageStore().apply {
            rearrangeResult = RearrangedImage("rearranged-uri", rearrangedRects)
        }
        val balloonRepository = FakeBalloonRepository()
        balloonRepository.upsertBalloon(
            1L,
            Balloon(id = 0, type = BalloonType.SPEAK, centerX = 0.24f, centerY = 0.76f),
        )
        val panelRepository = FakePanelRepository()
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
        panelRepository.replacePanels(1L, listOf(first, second, third))

        viewModel.uiState.test {
            while (awaitItem().panels.size < 3) { /* await seeded panels */ }

            val destination = third.copy(left = -0.4f, top = 0f)
            viewModel.onMoveImage(third, destination)
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals("rearranged-uri", state.imageUri)
            assertEquals(rearrangedRects, state.panels)
            assertEquals(
                listOf("existing-uri", listOf(first, second, third), 2, destination),
                imageStore.lastRearrangeRequest,
            )
            assertEquals(listOf("existing-uri"), imageStore.deleted)
            assertEquals(0.24f, state.balloons.single().centerX, 0.0001f)
            assertEquals(0.24f, state.balloons.single().centerY, 0.0001f)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `moving a panel does not show the blocking image overlay`() = runTest {
        val panel = RectFraction(0f, 0f, 0.48f, 1f)
        val destination = panel.copy(left = 0.52f)
        val gate = CompletableDeferred<Unit>()
        val imageStore = FakeImageStore().apply {
            rearrangeGate = gate
            rearrangeResult = RearrangedImage("rearranged-uri", listOf(destination))
        }
        val panelRepository = FakePanelRepository()
        val viewModel = ProjectViewModel(
            savedStateHandle = SavedStateHandle(mapOf("projectId" to 1L)),
            projectRepository = FakeProjectRepository(
                initial = listOf(
                    Project(id = 1, name = "Comic", description = "", createdAt = 1, imageUri = "existing-uri"),
                ),
            ),
            balloonRepository = FakeBalloonRepository(),
            panelRepository = panelRepository,
            imageStore = imageStore,
            settingsRepository = FakeSettingsRepository(),
        )
        panelRepository.replacePanels(1L, listOf(panel))
        viewModel.uiState.first { it.panels == listOf(panel) }

        viewModel.onMoveImage(panel, destination)
        runCurrent()

        assertTrue(imageStore.rearrangeStarted)
        assertEquals(false, viewModel.uiState.value.isProcessingImage)
        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `cropping a panel replaces the image and preserves panel rectangles`() = runTest {
        val panel = RectFraction(0f, 0f, 0.5f, 1f)
        val source = RectFraction(0.1f, 0.2f, 0.35f, 0.7f)
        val imageStore = FakeImageStore().apply { cropResult = "cropped-uri" }
        val panelRepository = FakePanelRepository()
        val viewModel = ProjectViewModel(
            savedStateHandle = SavedStateHandle(mapOf("projectId" to 1L)),
            projectRepository = FakeProjectRepository(
                initial = listOf(
                    Project(id = 1, name = "Comic", description = "", createdAt = 1, imageUri = "existing-uri"),
                ),
            ),
            balloonRepository = FakeBalloonRepository(),
            panelRepository = panelRepository,
            imageStore = imageStore,
            settingsRepository = FakeSettingsRepository(),
        )
        panelRepository.replacePanels(1L, listOf(panel))
        viewModel.uiState.first { it.panels == listOf(panel) }

        viewModel.onCropImage(panel, source)
        advanceUntilIdle()

        assertEquals("cropped-uri", viewModel.uiState.value.imageUri)
        assertEquals(listOf(panel), viewModel.uiState.value.panels)
        assertEquals(listOf("existing-uri", panel, source), imageStore.lastCropRequest)
        assertEquals(listOf("existing-uri"), imageStore.deleted)
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
    fun `adds a balloon at the center of the target image`() = runTest {
        val viewModel = viewModel()
        val target = RectFraction(left = 0.5f, top = 0.25f, width = 0.25f, height = 0.5f)

        viewModel.uiState.test {
            viewModel.addBalloon(BalloonType.SPEAK, target)
            advanceUntilIdle()

            val balloon = expectMostRecentItem().balloons.single()
            assertEquals(0.625f, balloon.centerX, 0.0001f)
            assertEquals(0.5f, balloon.centerY, 0.0001f)
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
