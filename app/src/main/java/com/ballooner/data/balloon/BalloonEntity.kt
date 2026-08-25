package com.ballooner.data.balloon

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ballooner.data.project.ProjectEntity

@Entity(
    tableName = "balloon",
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
data class BalloonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val type: String,
    val text: String,
    val centerX: Float,
    val centerY: Float,
    val width: Float,
    val height: Float,
    val tailAngleDegrees: Float,
    val tailLength: Float,
)
