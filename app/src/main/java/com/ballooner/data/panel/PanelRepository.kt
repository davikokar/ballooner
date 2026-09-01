package com.ballooner.data.panel

import com.ballooner.domain.model.RectFraction
import kotlinx.coroutines.flow.Flow

interface PanelRepository {
    fun observePanels(projectId: Long): Flow<List<RectFraction>>

    /** Replaces a project's whole panel layout, e.g. after compositing in a new image. */
    suspend fun replacePanels(projectId: Long, panels: List<RectFraction>)
}
