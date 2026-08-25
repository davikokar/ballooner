package com.ballooner.ui.projectlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ballooner.data.project.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectListViewModel @Inject constructor(
    private val repository: ProjectRepository,
) : ViewModel() {

    val uiState: StateFlow<ProjectListUiState> =
        repository.observeProjects()
            .map { projects ->
                if (projects.isEmpty()) {
                    ProjectListUiState.Empty
                } else {
                    ProjectListUiState.Content(projects)
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ProjectListUiState.Loading,
            )

    fun deleteProject(id: Long) {
        viewModelScope.launch { repository.deleteProject(id) }
    }
}
