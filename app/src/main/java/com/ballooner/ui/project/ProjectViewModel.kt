package com.ballooner.ui.project

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ballooner.data.balloon.BalloonRepository
import com.ballooner.data.image.ImageStore
import com.ballooner.data.panel.PanelRepository
import com.ballooner.data.project.ProjectRepository
import com.ballooner.data.settings.SettingsRepository
import com.ballooner.domain.model.AppSettings
import com.ballooner.domain.model.Balloon
import com.ballooner.domain.model.BalloonType
import com.ballooner.domain.model.ImagePlacement
import com.ballooner.domain.model.RectFraction
import com.ballooner.domain.model.TextSizeMode
import com.ballooner.domain.model.panelsInReadingOrder
import com.ballooner.domain.model.remappedFrom
import com.ballooner.domain.model.retainedCanvasRect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val projectRepository: ProjectRepository,
    private val balloonRepository: BalloonRepository,
    private val panelRepository: PanelRepository,
    private val imageStore: ImageStore,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val projectId: Long = savedStateHandle.get<Long>(PROJECT_ID_KEY) ?: 0L

    // Selection is UI-only state, not persisted.
    private val selectedBalloonId = MutableStateFlow<Long?>(null)

    // True while an image import/compose is running, so the UI can show a spinner instead of
    // looking frozen (decoding/scaling full-resolution photos can take a moment).
    private val isProcessingImage = MutableStateFlow(false)

    // Kept hot so new balloons can read the default font even before the UI subscribes.
    private val settings = settingsRepository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    // combine() only has typed overloads up to 5 flows, so panels are folded in separately.
    private val baseUiState = combine(
        projectRepository.observeProject(projectId),
        balloonRepository.observeBalloons(projectId),
        selectedBalloonId,
        settings,
        isProcessingImage,
    ) { project, balloons, selectedId, appSettings, processingImage ->
        ProjectUiState(
            name = project?.name.orEmpty(),
            imageUri = project?.imageUri,
            balloons = balloons,
            selectedBalloonId = selectedId?.takeIf { id -> balloons.any { it.id == id } },
            hideFontSelector = appSettings.hideFontSelector,
            autoTextSize = appSettings.textSizeMode == TextSizeMode.AUTO,
            isProcessingImage = processingImage,
        )
    }

    val uiState: StateFlow<ProjectUiState> = combine(
        baseUiState,
        panelRepository.observePanels(projectId),
    ) { state, panels -> state.copy(panels = panels) }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProjectUiState(),
    )

    fun onImagePicked(sourceUri: String) {
        viewModelScope.launch {
            isProcessingImage.value = true
            try {
                val previous = uiState.value.imageUri
                val local = imageStore.importImage(sourceUri) ?: return@launch
                projectRepository.setProjectImage(projectId, local)
                // A plain replace starts a fresh single-panel layout.
                panelRepository.replacePanels(projectId, listOf(RectFraction(0f, 0f, 1f, 1f)))
                if (previous != null && previous != local) imageStore.deleteImage(previous)
            } finally {
                isProcessingImage.value = false
            }
        }
    }

    fun onInitialImagesPicked(sourceUris: List<String>) {
        if (sourceUris.isEmpty() || uiState.value.imageUri != null) return
        viewModelScope.launch {
            isProcessingImage.value = true
            try {
                if (sourceUris.size == 1) {
                    val local = imageStore.importImage(sourceUris.single()) ?: return@launch
                    projectRepository.setProjectImage(projectId, local)
                    panelRepository.replacePanels(projectId, listOf(RectFraction(0f, 0f, 1f, 1f)))
                } else {
                    val grid = imageStore.createInitialGrid(sourceUris, settings.value.layoutColumns)
                        ?: return@launch
                    projectRepository.setProjectImage(projectId, grid.uri)
                    panelRepository.replacePanels(projectId, grid.panelRects)
                }
            } finally {
                isProcessingImage.value = false
            }
        }
    }

    /**
     * Adds [sourceUri] as a new panel next to the comic's current image, growing the canvas,
     * remapping existing panels and balloons so they keep their place on the (now smaller,
     * relative) old image, and persisting the new panel layout.
     */
    fun onAddImage(sourceUri: String, placement: ImagePlacement) {
        viewModelScope.launch {
            isProcessingImage.value = true
            try {
                val previous = uiState.value.imageUri ?: return@launch
                val composed = imageStore.composeImages(previous, sourceUri, placement)
                    ?: return@launch
                val rect = composed.previousImageRect
                uiState.value.balloons.forEach { balloon ->
                    balloonRepository.upsertBalloon(projectId, balloon.remappedInto(rect))
                }
                val remappedPanels = uiState.value.panels.map { it.remappedInto(rect) }
                panelRepository.replacePanels(projectId, remappedPanels + composed.newImageRect)
                projectRepository.setProjectImage(projectId, composed.uri)
                imageStore.deleteImage(previous)
            } finally {
                isProcessingImage.value = false
            }
        }
    }

    private fun Balloon.remappedInto(rect: RectFraction) = copy(
        centerX = rect.left + centerX * rect.width,
        centerY = rect.top + centerY * rect.height,
        width = width * rect.width,
        height = height * rect.height,
        // tailLength is a fraction of the canvas's smaller dimension; approximate its new
        // fraction with the smaller of the two axis scale factors.
        tailLength = tailLength * minOf(rect.width, rect.height),
    )

    private fun RectFraction.remappedInto(rect: RectFraction) = RectFraction(
        left = rect.left + left * rect.width,
        top = rect.top + top * rect.height,
        width = width * rect.width,
        height = height * rect.height,
    )

    /**
        * Removes [panel], crops empty outer space, and remaps surviving panels and balloons.
        * No-ops for the comic's only panel; deleting that is handled by [deleteProject].
     */
    fun onDeleteImage(panel: RectFraction) {
        if (uiState.value.panels.size <= 1) return
        viewModelScope.launch {
            isProcessingImage.value = true
            try {
                val previous = uiState.value.imageUri ?: return@launch
                val remainingPanels = uiState.value.panels - panel
                val retained = retainedCanvasRect(remainingPanels)
                val cropped = imageStore.removeRegion(previous, panel, retained) ?: return@launch
                uiState.value.balloons.forEach { balloon ->
                    if (panel.contains(balloon.centerX, balloon.centerY)) {
                        balloonRepository.deleteBalloon(balloon.id)
                    } else {
                        balloonRepository.upsertBalloon(projectId, balloon.remappedFrom(retained))
                    }
                }
                panelRepository.replacePanels(projectId, remainingPanels.map { it.remappedFrom(retained) })
                projectRepository.setProjectImage(projectId, cropped)
                imageStore.deleteImage(previous)
            } finally {
                isProcessingImage.value = false
            }
        }
    }

    private fun Balloon.remappedFrom(rect: RectFraction) = copy(
        centerX = (centerX - rect.left) / rect.width,
        centerY = (centerY - rect.top) / rect.height,
        width = width / rect.width,
        height = height / rect.height,
        tailLength = tailLength / minOf(rect.width, rect.height),
    )

    /** Inserts [panel] at [target]'s reading-order position and shifts the intervening panels. */
    fun onMoveImage(panel: RectFraction, placement: ImagePlacement) {
        if (panel == placement.anchor) return
        viewModelScope.launch {
            isProcessingImage.value = true
            try {
                val previous = uiState.value.imageUri ?: return@launch
                val orderedPanels = panelsInReadingOrder(uiState.value.panels)
                val fromIndex = orderedPanels.indexOf(panel).takeIf { it >= 0 } ?: return@launch
                val targetIndex = orderedPanels.indexOf(placement.anchor).takeIf { it >= 0 } ?: return@launch
                val rearranged = imageStore.rearrangePanels(
                    previous,
                    orderedPanels,
                    fromIndex,
                    targetIndex,
                    placement.position,
                )
                    ?: return@launch
                uiState.value.balloons.forEach { balloon ->
                    val panelIndex = orderedPanels.indexOfFirst { it.contains(balloon.centerX, balloon.centerY) }
                    if (panelIndex >= 0) {
                        balloonRepository.upsertBalloon(
                            projectId,
                            balloon.remappedBetween(orderedPanels[panelIndex], rearranged.panelRects[panelIndex]),
                        )
                    }
                }
                panelRepository.replacePanels(projectId, rearranged.panelRects)
                projectRepository.setProjectImage(projectId, rearranged.uri)
                imageStore.deleteImage(previous)
            } finally {
                isProcessingImage.value = false
            }
        }
    }

    private fun Balloon.remappedBetween(from: RectFraction, to: RectFraction) = copy(
        centerX = to.left + (centerX - from.left) / from.width * to.width,
        centerY = to.top + (centerY - from.top) / from.height * to.height,
        width = width / from.width * to.width,
        height = height / from.height * to.height,
        tailLength = tailLength * minOf(to.width / from.width, to.height / from.height),
    )

    fun setProjectName(name: String) {
        viewModelScope.launch { projectRepository.setProjectName(projectId, name) }
    }

    /** Deletes this project (and its copied image), then invokes [onDeleted] for navigation. */
    fun deleteProject(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val imageUri = uiState.value.imageUri
            projectRepository.deleteProject(projectId)
            imageUri?.let { imageStore.deleteImage(it) }
            onDeleted()
        }
    }

    fun addBalloon(type: BalloonType, targetPanel: RectFraction? = null) {
        viewModelScope.launch {
            val initial = if (type == BalloonType.CAPTION) {
                Balloon(id = 0, type = type, font = settings.value.defaultFont, tailLength = 0f, cornerRoundness = 0f)
            } else {
                Balloon(id = 0, type = type, font = settings.value.defaultFont)
            }
            val balloon = targetPanel?.let { panel ->
                initial.copy(
                    centerX = panel.left + panel.width / 2f,
                    centerY = panel.top + panel.height / 2f,
                )
            } ?: initial
            val id = balloonRepository.upsertBalloon(projectId, balloon)
            selectedBalloonId.value = id
        }
    }

    fun selectBalloon(id: Long?) {
        selectedBalloonId.value = id
    }

    fun deleteSelectedBalloon() {
        val id = selectedBalloonId.value ?: return
        viewModelScope.launch {
            balloonRepository.deleteBalloon(id)
            selectedBalloonId.value = null
        }
    }

    /** Persists a balloon after a direct-manipulation gesture (move / resize / tail / text). */
    fun commitBalloon(balloon: Balloon) {
        viewModelScope.launch { balloonRepository.upsertBalloon(projectId, balloon.sanitized()) }
    }

    private fun Balloon.sanitized() = copy(
        centerX = centerX.coerceIn(0f, 1f),
        centerY = centerY.coerceIn(0f, 1f),
        width = width.coerceIn(MIN_SIZE, 1f),
        height = height.coerceIn(MIN_SIZE, 1f),
        tailAngleDegrees = tailAngleDegrees.mod(360f),
        tailLength = tailLength.coerceIn(0f, MAX_TAIL_LENGTH),
        cornerRoundness = cornerRoundness.coerceIn(0f, 1f),
        tailWidth = tailWidth.coerceIn(MIN_TAIL_WIDTH, MAX_TAIL_WIDTH),
        fontSize = fontSize.coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE),
    )

    private companion object {
        const val PROJECT_ID_KEY = "projectId"
        const val MIN_SIZE = 0.1f
        const val MAX_TAIL_LENGTH = 0.4f
        const val MIN_TAIL_WIDTH = 0.1f
        const val MAX_TAIL_WIDTH = 1.5f
        const val MIN_FONT_SIZE = 8f
        const val MAX_FONT_SIZE = 48f
    }
}
