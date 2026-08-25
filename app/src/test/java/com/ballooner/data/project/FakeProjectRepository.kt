package com.ballooner.data.project

import com.ballooner.domain.model.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Hand-written fake so tests never touch Room or the real database. */
class FakeProjectRepository(
    initial: List<Project> = emptyList(),
) : ProjectRepository {

    private val projects = MutableStateFlow(initial)

    override fun observeProjects(): Flow<List<Project>> = projects

    override fun observeProject(id: Long): Flow<Project?> =
        projects.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun createProject(name: String, description: String): Long {
        val id = (projects.value.maxOfOrNull { it.id } ?: 0L) + 1L
        projects.value = projects.value + Project(
            id = id,
            name = name.trim(),
            description = description.trim(),
            createdAt = id,
        )
        return id
    }

    override suspend fun setProjectImage(id: Long, uri: String?) {
        projects.value = projects.value.map { if (it.id == id) it.copy(imageUri = uri) else it }
    }

    override suspend fun deleteProject(id: Long) {
        projects.value = projects.value.filterNot { it.id == id }
    }
}
