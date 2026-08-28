package com.ballooner.ui.project

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ballooner.data.balloon.BalloonRepository
import com.ballooner.data.image.ImageStore
import com.ballooner.data.image.RectFraction
import com.ballooner.data.project.ProjectRepository
import com.ballooner.data.settings.SettingsRepository
import com.ballooner.domain.model.AppSettings
import com.ballooner.domain.model.Balloon
import com.ballooner.domain.model.BalloonType
import com.ballooner.domain.model.ImagePosition
import com.ballooner.domain.model.TextSizeMode
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

    val uiState: StateFlow<ProjectUiState> = combine(
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
    }.stateIn(
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
                if (previous != null && previous != local) imageStore.deleteImage(previous)
            } finally {
                isProcessingImage.value = false
            }
        }
    }

    /**
     * Adds [sourceUri] as a new panel next to the comic's current image, growing the canvas and
     * remapping existing balloons so they keep their place on the (now smaller, relative) old image.
     */
    fun onAddImage(sourceUri: String, position: ImagePosition) {
        viewModelScope.launch {
            isProcessingImage.value = true
            try {
                val previous = uiState.value.imageUri ?: return@launch
                val composed = imageStore.composeImages(previous, sourceUri, position) ?: return@launch
                val rect = composed.previousImageRect
                uiState.value.balloons.forEach { balloon ->
                    balloonRepository.upsertBalloon(projectId, balloon.remappedInto(rect))
                }
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

    fun addBalloon(type: BalloonType) {
        viewModelScope.launch {
            val balloon = if (type == BalloonType.CAPTION) {
                Balloon(id = 0, type = type, font = settings.value.defaultFont, tailLength = 0f, cornerRoundness = 0f)
            } else {
                Balloon(id = 0, type = type, font = settings.value.defaultFont)
            }
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
