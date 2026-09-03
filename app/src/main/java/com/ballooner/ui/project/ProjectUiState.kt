package com.ballooner.ui.project

import com.ballooner.domain.model.Balloon
import com.ballooner.domain.model.RectFraction

data class ProjectUiState(
    val name: String = "",
    val imageUri: String? = null,
    val balloons: List<Balloon> = emptyList(),
    val selectedBalloonId: Long? = null,
    val hideFontSelector: Boolean = false,
    val autoTextSize: Boolean = false,
    // True while an image import/compose is running in the background.
    val isProcessingImage: Boolean = false,
    val canUndo: Boolean = false,
    // Each existing image panel's rect within the current merged image.
    val panels: List<RectFraction> = emptyList(),
) {
    val hasImage: Boolean get() = imageUri != null

    val selectedBalloon: Balloon?
        get() = balloons.firstOrNull { it.id == selectedBalloonId }
}
