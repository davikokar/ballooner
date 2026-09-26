package com.ballooner.data.comic

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

/** A comic: one page, its style, and its grid. Cuts, panels, and balloons hang off it. */
@Entity(tableName = "comic")
data class ComicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val pageShape: String,
    val pageMargin: Float,
    val gutter: Float,
    val borderThickness: Float,
    val cornerRadius: Float,
    val rows: Int,
    val columns: Int,
    val rowWeights: List<Float>,
    val columnWeights: List<Float>,
)

/** One merged panel of the grid. Unmerged cells are not stored; they are implied. */
@Entity(
    tableName = "comic_span",
    primaryKeys = ["comicId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = ComicEntity::class,
            parentColumns = ["id"],
            childColumns = ["comicId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("comicId")],
)
data class ComicSpanEntity(
    val comicId: Long,
    val position: Int,
    val firstRow: Int,
    val firstColumn: Int,
    val rowCount: Int,
    val columnCount: Int,
)

/** One cut. [position] preserves the order they were drawn in, which decides the panel shapes. */
@Entity(
    tableName = "comic_cut",
    primaryKeys = ["comicId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = ComicEntity::class,
            parentColumns = ["id"],
            childColumns = ["comicId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("comicId")],
)
data class ComicCutEntity(
    val comicId: Long,
    val position: Int,
    val aU: Float,
    val aV: Float,
    val bU: Float,
    val bV: Float,
    // Null for a page cut; otherwise the anchor that picks the panel this cut splits.
    val anchorU: Float?,
    val anchorV: Float?,
)

/** What fills one panel. [position] is the panel's index in reading order. */
@Entity(
    tableName = "comic_panel",
    primaryKeys = ["comicId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = ComicEntity::class,
            parentColumns = ["id"],
            childColumns = ["comicId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("comicId")],
)
data class ComicPanelEntity(
    val comicId: Long,
    val position: Int,
    val sourceUri: String?,
    val centreU: Float,
    val centreV: Float,
    val zoom: Float,
    val angleDegrees: Float,
)

/** One balloon. [balloonId] rises with each balloon added, so it is also the drawing order. */
@Entity(
    tableName = "comic_balloon",
    primaryKeys = ["comicId", "balloonId"],
    foreignKeys = [
        ForeignKey(
            entity = ComicEntity::class,
            parentColumns = ["id"],
            childColumns = ["comicId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("comicId")],
)
data class ComicBalloonEntity(
    val comicId: Long,
    val balloonId: Long,
    // Null for a comic balloon; otherwise the index of the panel it belongs to.
    val panelIndex: Int?,
    val type: String,
    val text: String,
    val centreU: Float,
    val centreV: Float,
    val width: Float,
    val height: Float,
    val tailAngleDegrees: Float,
    val tailLength: Float,
    val tailWidth: Float,
    val cornerRoundness: Float,
    val fontSize: Float,
    val font: String,
)

/** Stores grid weights as a plain comma-separated list, which needs no serialization library. */
class FloatListConverter {

    @TypeConverter
    fun toFloatList(value: String): List<Float> =
        if (value.isEmpty()) emptyList() else value.split(',').map(String::toFloat)

    @TypeConverter
    fun fromFloatList(value: List<Float>): String = value.joinToString(",")
}
