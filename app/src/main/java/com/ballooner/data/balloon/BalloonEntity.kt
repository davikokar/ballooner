package com.ballooner.data.balloon

import androidx.room.ColumnInfo
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
    @ColumnInfo(defaultValue = "1.0") val cornerRoundness: Float = 1f,
    @ColumnInfo(defaultValue = "0.5") val tailWidth: Float = 0.5f,
    @ColumnInfo(defaultValue = "14.0") val fontSize: Float = 14f,
    @ColumnInfo(defaultValue = "'DEFAULT'") val font: String = "DEFAULT",
)
