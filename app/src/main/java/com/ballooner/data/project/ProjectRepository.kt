package com.ballooner.data.project

import com.ballooner.domain.model.Project
import kotlinx.coroutines.flow.Flow

/**
 * Domain-facing gateway for projects. ViewModels depend on this, never on the
 * DAO directly.
 */
interface ProjectRepository {
    fun observeProjects(): Flow<List<Project>>

    suspend fun createProject(name: String, description: String): Long

    suspend fun deleteProject(id: Long)
}
