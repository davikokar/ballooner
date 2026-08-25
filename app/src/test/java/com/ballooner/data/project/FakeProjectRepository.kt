package com.ballooner.data.project

import com.ballooner.domain.model.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Hand-written fake so tests never touch Room or the real database. */
class FakeProjectRepository(
    initial: List<Project> = emptyList(),
) : ProjectRepository {

    private val projects = MutableStateFlow(initial)

    override fun observeProjects(): Flow<List<Project>> = projects

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

    override suspend fun deleteProject(id: Long) {
        projects.value = projects.value.filterNot { it.id == id }
    }
}
