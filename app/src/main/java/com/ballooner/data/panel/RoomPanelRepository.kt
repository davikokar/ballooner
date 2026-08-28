package com.ballooner.data.panel

import com.ballooner.domain.model.RectFraction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomPanelRepository @Inject constructor(
    private val dao: PanelDao,
) : PanelRepository {

    override fun observePanels(projectId: Long): Flow<List<RectFraction>> =
        dao.observeByProject(projectId).map { entities -> entities.map(PanelEntity::toDomain) }

    override suspend fun replacePanels(projectId: Long, panels: List<RectFraction>) {
        dao.replaceAll(projectId, panels.map { it.toEntity(projectId) })
    }
}

private fun PanelEntity.toDomain() = RectFraction(left = left, top = top, width = width, height = height)

private fun RectFraction.toEntity(projectId: Long) = PanelEntity(
    projectId = projectId,
    left = left,
    top = top,
    width = width,
    height = height,
)
