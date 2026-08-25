package com.ballooner.ui.projectcreate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ballooner.data.project.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectCreateViewModel @Inject constructor(
    private val repository: ProjectRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectCreateUiState())
    val uiState: StateFlow<ProjectCreateUiState> = _uiState.asStateFlow()

    fun onNameChange(name: String) = _uiState.update { it.copy(name = name) }

    fun onDescriptionChange(description: String) =
        _uiState.update { it.copy(description = description) }

    fun save() {
        val current = _uiState.value
        if (!current.canSave) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            repository.createProject(current.name, current.description)
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }
}
