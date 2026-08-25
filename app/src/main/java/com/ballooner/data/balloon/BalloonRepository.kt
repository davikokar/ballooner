package com.ballooner.data.balloon

import com.ballooner.domain.model.Balloon
import kotlinx.coroutines.flow.Flow

interface BalloonRepository {
    fun observeBalloons(projectId: Long): Flow<List<Balloon>>

    /** Inserts a new balloon (id == 0) or updates an existing one; returns its id. */
    suspend fun upsertBalloon(projectId: Long, balloon: Balloon): Long

    suspend fun deleteBalloon(id: Long)
}
