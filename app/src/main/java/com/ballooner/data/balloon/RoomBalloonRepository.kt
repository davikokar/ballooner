package com.ballooner.data.balloon

import com.ballooner.data.project.ProjectDao
import com.ballooner.domain.model.Balloon
import com.ballooner.domain.model.BalloonFont
import com.ballooner.domain.model.BalloonType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomBalloonRepository @Inject constructor(
    private val dao: BalloonDao,
    private val projectDao: ProjectDao,
) : BalloonRepository {

    override fun observeBalloons(projectId: Long): Flow<List<Balloon>> =
        dao.observeByProject(projectId).map { entities -> entities.map(BalloonEntity::toDomain) }

    override suspend fun upsertBalloon(projectId: Long, balloon: Balloon): Long {
        val id = dao.upsert(balloon.toEntity(projectId))
        projectDao.touch(projectId, System.currentTimeMillis())
        return id
    }

    override suspend fun deleteBalloon(id: Long) {
        val projectId = dao.projectIdOf(id)
        dao.deleteById(id)
        if (projectId != null) projectDao.touch(projectId, System.currentTimeMillis())
    }
}

private fun BalloonEntity.toDomain() = Balloon(
    id = id,
    type = BalloonType.valueOf(type),
    text = text,
    centerX = centerX,
    centerY = centerY,
    width = width,
    height = height,
    tailAngleDegrees = tailAngleDegrees,
    tailLength = tailLength,
    cornerRoundness = cornerRoundness,
    tailWidth = tailWidth,
    fontSize = fontSize,
    font = BalloonFont.valueOf(font),
)

private fun Balloon.toEntity(projectId: Long) = BalloonEntity(
    id = id,
    projectId = projectId,
    type = type.name,
    text = text,
    centerX = centerX,
    centerY = centerY,
    width = width,
    height = height,
    tailAngleDegrees = tailAngleDegrees,
    tailLength = tailLength,
    cornerRoundness = cornerRoundness,
    tailWidth = tailWidth,
    fontSize = fontSize,
    font = font.name,
)
