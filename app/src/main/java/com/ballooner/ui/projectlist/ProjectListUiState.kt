package com.ballooner.ui.projectlist

import com.ballooner.domain.model.Project

sealed interface ProjectListUiState {
    data object Loading : ProjectListUiState
    data object Empty : ProjectListUiState
    data class Content(val projects: List<Project>) : ProjectListUiState
}
