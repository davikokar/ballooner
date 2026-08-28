package com.ballooner.data.panel

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ballooner.data.project.ProjectEntity

@Entity(
    tableName = "panel",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId")],
)
data class PanelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    // Fractions (0f..1f) of the project's current merged image.
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
)
