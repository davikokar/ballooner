package com.ballooner.data.comic

import com.ballooner.domain.comic.Balloon
import com.ballooner.domain.comic.BalloonScope
import com.ballooner.domain.comic.Comic
import com.ballooner.domain.comic.ComicStyle
import com.ballooner.domain.comic.Cut
import com.ballooner.domain.comic.CutScope
import com.ballooner.domain.comic.Grid
import com.ballooner.domain.comic.Layout
import com.ballooner.domain.comic.NormalizedPoint
import com.ballooner.domain.comic.PageShape
import com.ballooner.domain.comic.Panel
import com.ballooner.domain.comic.PanelImage
import com.ballooner.domain.comic.Span
import com.ballooner.domain.model.BalloonFont
import com.ballooner.domain.model.BalloonType

fun ComicWithParts.toDomain(): Comic = Comic(
    name = comic.name,
    pageShape = enumValueOrDefault(comic.pageShape, PageShape.PORTRAIT),
    style = ComicStyle(
        pageMargin = comic.pageMargin,
        gutter = comic.gutter,
        borderThickness = comic.borderThickness,
        cornerRadius = comic.cornerRadius,
    ),
    layout = Layout(
        grid = Grid(
            rows = comic.rows,
            columns = comic.columns,
            rowWeights = comic.rowWeights.ifEmpty { List(comic.rows) { 1f } },
            columnWeights = comic.columnWeights.ifEmpty { List(comic.columns) { 1f } },
            spans = spans.sortedBy { it.position }.map { it.toDomain() },
        ),
        cuts = cuts.sortedBy { it.position }.map { it.toDomain() },
    ),
    panels = panels.sortedBy { it.position }.map { it.toDomain() },
    balloons = balloons.sortedBy { it.balloonId }.map { it.toDomain() },
)

fun Comic.toParts(id: Long, createdAt: Long, updatedAt: Long): ComicWithParts = ComicWithParts(
    comic = ComicEntity(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        pageShape = pageShape.name,
        pageMargin = style.pageMargin,
        gutter = style.gutter,
        borderThickness = style.borderThickness,
        cornerRadius = style.cornerRadius,
        rows = layout.grid.rows,
        columns = layout.grid.columns,
        rowWeights = layout.grid.rowWeights,
        columnWeights = layout.grid.columnWeights,
    ),
    spans = layout.grid.spans.mapIndexed { position, span ->
        ComicSpanEntity(
            comicId = id,
            position = position,
            firstRow = span.firstRow,
            firstColumn = span.firstColumn,
            rowCount = span.rowCount,
            columnCount = span.columnCount,
        )
    },
    cuts = layout.cuts.mapIndexed { position, cut ->
        val anchor = (cut.scope as? CutScope.AtPoint)?.anchor
        ComicCutEntity(
            comicId = id,
            position = position,
            aU = cut.a.u,
            aV = cut.a.v,
            bU = cut.b.u,
            bV = cut.b.v,
            anchorU = anchor?.u,
            anchorV = anchor?.v,
        )
    },
    panels = panels.mapIndexed { position, panel ->
        ComicPanelEntity(
            comicId = id,
            position = position,
            sourceUri = panel.image?.sourceUri,
            centreU = panel.image?.centre?.u ?: 0.5f,
            centreV = panel.image?.centre?.v ?: 0.5f,
            zoom = panel.image?.zoom ?: 1f,
            angleDegrees = panel.image?.angleDegrees ?: 0f,
        )
    },
    balloons = balloons.map { balloon ->
        ComicBalloonEntity(
            comicId = id,
            balloonId = balloon.id,
            panelIndex = (balloon.scope as? BalloonScope.Panel)?.panelIndex,
            type = balloon.type.name,
            text = balloon.text,
            centreU = balloon.centre.u,
            centreV = balloon.centre.v,
            width = balloon.width,
            height = balloon.height,
            tailAngleDegrees = balloon.tailAngleDegrees,
            tailLength = balloon.tailLength,
            tailWidth = balloon.tailWidth,
            cornerRoundness = balloon.cornerRoundness,
            fontSize = balloon.fontSize,
            font = balloon.font.name,
        )
    },
)

private fun ComicSpanEntity.toDomain() = Span(firstRow, firstColumn, rowCount, columnCount)

private fun ComicCutEntity.toDomain() = Cut(
    a = NormalizedPoint(aU, aV),
    b = NormalizedPoint(bU, bV),
    scope = if (anchorU != null && anchorV != null) {
        CutScope.AtPoint(NormalizedPoint(anchorU, anchorV))
    } else {
        CutScope.WholePage
    },
)

private fun ComicPanelEntity.toDomain() = Panel(
    image = sourceUri?.let {
        PanelImage(
            sourceUri = it,
            centre = NormalizedPoint(centreU, centreV),
            zoom = zoom,
            angleDegrees = angleDegrees,
        )
    },
)

private fun ComicBalloonEntity.toDomain() = Balloon(
    id = balloonId,
    type = enumValueOrDefault(type, BalloonType.SPEAK),
    scope = panelIndex?.let { BalloonScope.Panel(it) } ?: BalloonScope.Comic,
    text = text,
    centre = NormalizedPoint(centreU, centreV),
    width = width,
    height = height,
    tailAngleDegrees = tailAngleDegrees,
    tailLength = tailLength,
    tailWidth = tailWidth,
    cornerRoundness = cornerRoundness,
    fontSize = fontSize,
    font = enumValueOrDefault(font, BalloonFont.DEFAULT),
)

private inline fun <reified T : Enum<T>> enumValueOrDefault(name: String, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: fallback
