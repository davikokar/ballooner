package com.ballooner.ui.project

import com.ballooner.domain.model.Balloon

data class ProjectUiState(
    val name: String = "",
    val imageUri: String? = null,
    val balloons: List<Balloon> = emptyList(),
    val selectedBalloonId: Long? = null,
    val hideFontSelector: Boolean = false,
    val autoTextSize: Boolean = false,
    // True while an image import/compose is running in the background.
    val isProcessingImage: Boolean = false,
) {
    val hasImage: Boolean get() = imageUri != null

    val selectedBalloon: Balloon?
        get() = balloons.firstOrNull { it.id == selectedBalloonId }
}
