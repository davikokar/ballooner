package com.ballooner.data.project

import com.ballooner.domain.model.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomProjectRepository @Inject constructor(
    private val dao: ProjectDao,
) : ProjectRepository {

    override fun observeProjects(): Flow<List<Project>> =
        dao.observeAll().map { entities -> entities.map(ProjectEntity::toDomain) }

    override suspend fun createProject(name: String, description: String): Long =
        dao.insert(
            ProjectEntity(
                name = name.trim(),
                description = description.trim(),
                createdAt = System.currentTimeMillis(),
            ),
        )

    override suspend fun deleteProject(id: Long) = dao.deleteById(id)
}

private fun ProjectEntity.toDomain() = Project(
    id = id,
    name = name,
    description = description,
    createdAt = createdAt,
)
