package com.ballooner.ui.project

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ballooner.data.balloon.BalloonRepository
import com.ballooner.data.project.ProjectRepository
import com.ballooner.domain.model.Balloon
import com.ballooner.domain.model.BalloonType
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
) : ViewModel() {

    private val projectId: Long = savedStateHandle.get<Long>(PROJECT_ID_KEY) ?: 0L

    // Selection is UI-only state, not persisted.
    private val selectedBalloonId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<ProjectUiState> = combine(
        projectRepository.observeProject(projectId),
        balloonRepository.observeBalloons(projectId),
        selectedBalloonId,
    ) { project, balloons, selectedId ->
        ProjectUiState(
            name = project?.name.orEmpty(),
            imageUri = project?.imageUri,
            balloons = balloons,
            selectedBalloonId = selectedId?.takeIf { id -> balloons.any { it.id == id } },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProjectUiState(),
    )

    fun onImagePicked(uri: String) {
        viewModelScope.launch { projectRepository.setProjectImage(projectId, uri) }
    }

    fun setProjectName(name: String) {
        viewModelScope.launch { projectRepository.setProjectName(projectId, name) }
    }

    fun addBalloon(type: BalloonType) {
        viewModelScope.launch {
            val id = balloonRepository.upsertBalloon(projectId, Balloon(id = 0, type = type))
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
