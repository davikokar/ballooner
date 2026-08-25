package com.ballooner.ui.projectcreate

data class ProjectCreateUiState(
    val name: String = "",
    val description: String = "",
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
) {
    val canSave: Boolean get() = name.isNotBlank() && !isSaving
}
