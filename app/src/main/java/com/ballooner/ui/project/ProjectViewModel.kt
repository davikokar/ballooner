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

    fun setType(type: BalloonType) = updateSelected { it.copy(type = type) }

    fun setText(text: String) = updateSelected { it.copy(text = text) }

    fun setSize(width: Float, height: Float) = updateSelected {
        it.copy(width = width.coerceIn(MIN_SIZE, 1f), height = height.coerceIn(MIN_SIZE, 1f))
    }

    fun setTailAngle(degrees: Float) = updateSelected {
        it.copy(tailAngleDegrees = degrees.mod(360f))
    }

    fun setTailLength(length: Float) = updateSelected {
        it.copy(tailLength = length.coerceIn(0f, MAX_TAIL_LENGTH))
    }

    /** Moves the selected balloon by a delta expressed as a fraction of the image. */
    fun moveSelectedBy(dxFraction: Float, dyFraction: Float) = updateSelected {
        it.copy(
            centerX = (it.centerX + dxFraction).coerceIn(0f, 1f),
            centerY = (it.centerY + dyFraction).coerceIn(0f, 1f),
        )
    }

    private fun updateSelected(transform: (Balloon) -> Balloon) {
        val current = uiState.value.selectedBalloon ?: return
        viewModelScope.launch { balloonRepository.upsertBalloon(projectId, transform(current)) }
    }

    private companion object {
        const val PROJECT_ID_KEY = "projectId"
        const val MIN_SIZE = 0.1f
        const val MAX_TAIL_LENGTH = 0.4f
    }
}
