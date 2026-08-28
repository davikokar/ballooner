package com.ballooner.data.panel

import com.ballooner.domain.model.RectFraction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Hand-written fake so tests never touch Room or the real database. */
class FakePanelRepository : PanelRepository {

    private val stored = MutableStateFlow<Map<Long, List<RectFraction>>>(emptyMap())

    override fun observePanels(projectId: Long): Flow<List<RectFraction>> =
        stored.map { it[projectId].orEmpty() }

    override suspend fun replacePanels(projectId: Long, panels: List<RectFraction>) {
        stored.value = stored.value + (projectId to panels)
    }
}
