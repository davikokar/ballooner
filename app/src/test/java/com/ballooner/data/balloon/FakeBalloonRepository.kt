package com.ballooner.data.balloon

import com.ballooner.domain.model.Balloon
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Hand-written fake so tests never touch Room or the real database. */
class FakeBalloonRepository : BalloonRepository {

    private val stored = MutableStateFlow<List<Pair<Long, Balloon>>>(emptyList())
    private var nextId = 1L

    override fun observeBalloons(projectId: Long): Flow<List<Balloon>> =
        stored.map { list -> list.filter { it.first == projectId }.map { it.second } }

    override suspend fun upsertBalloon(projectId: Long, balloon: Balloon): Long {
        val id = if (balloon.id == 0L) nextId++ else balloon.id
        val saved = balloon.copy(id = id)
        stored.update { list -> list.filterNot { it.second.id == id } + (projectId to saved) }
        return id
    }

    override suspend fun deleteBalloon(id: Long) {
        stored.update { list -> list.filterNot { it.second.id == id } }
    }
}
