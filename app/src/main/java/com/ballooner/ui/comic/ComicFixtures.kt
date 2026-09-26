package com.ballooner.ui.comic

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
import com.ballooner.domain.comic.panelShapes
import com.ballooner.domain.model.BalloonType

/** A named comic used to check the renderer against a layout whose shape is known by hand. */
data class ComicFixture(val name: String, val comic: Comic)

private const val SAMPLE = "sample"

/**
 * Hand-built comics covering every layout feature, so the geometry can be looked at directly
 * before any editing UI exists to explain away a wrong result.
 */
fun comicFixtures(): List<ComicFixture> = listOf(
    ComicFixture("Single panel", fixture(PageShape.SQUARE, Layout(Grid(1, 1)))),
    ComicFixture("Strip of three", fixture(PageShape.LANDSCAPE, Layout(Grid(rows = 1, columns = 3)))),
    ComicFixture("Grid 3x3", fixture(PageShape.PORTRAIT, Layout(Grid(rows = 3, columns = 3)))),
    ComicFixture(
        name = "Merged top row and tall left panel",
        comic = fixture(
            PageShape.PORTRAIT,
            Layout(
                Grid(
                    rows = 3,
                    columns = 3,
                    spans = listOf(
                        Span(0, 0, rowCount = 1, columnCount = 3),
                        Span(1, 0, rowCount = 2, columnCount = 1),
                    ),
                ),
            ),
        ),
    ),
    ComicFixture(
        name = "Uneven weights",
        comic = fixture(
            PageShape.SQUARE,
            Layout(Grid(rows = 2, columns = 2, rowWeights = listOf(2f, 1f), columnWeights = listOf(1f, 3f))),
        ),
    ),
    ComicFixture(
        name = "Diagonal page cut",
        comic = fixture(
            PageShape.SQUARE,
            Layout(Grid(1, 1), listOf(Cut(NormalizedPoint(0f, 0.2f), NormalizedPoint(1f, 0.8f), CutScope.WholePage))),
        ),
    ),
    ComicFixture(
        name = "Panel cut: two above, three below",
        comic = fixture(
            PageShape.SQUARE,
            Layout(
                grid = Grid(rows = 2, columns = 1),
                cuts = listOf(
                    Cut(
                        a = NormalizedPoint(0.5f, 0f),
                        b = NormalizedPoint(0.5f, 1f),
                        scope = CutScope.AtPoint(NormalizedPoint(0.5f, 0.2f)),
                    ),
                    Cut(
                        a = NormalizedPoint(0.34f, 0f),
                        b = NormalizedPoint(0.34f, 1f),
                        scope = CutScope.AtPoint(NormalizedPoint(0.2f, 0.8f)),
                    ),
                    Cut(
                        a = NormalizedPoint(0.67f, 0f),
                        b = NormalizedPoint(0.67f, 1f),
                        scope = CutScope.AtPoint(NormalizedPoint(0.8f, 0.8f)),
                    ),
                ),
            ),
        ),
    ),
    ComicFixture(
        name = "Diagonal panel cut inside a grid",
        comic = fixture(
            PageShape.SQUARE,
            Layout(
                grid = Grid(rows = 2, columns = 2),
                cuts = listOf(
                    Cut(
                        a = NormalizedPoint(0.5f, 1f),
                        b = NormalizedPoint(1f, 0.5f),
                        scope = CutScope.AtPoint(NormalizedPoint(0.75f, 0.75f)),
                    ),
                ),
            ),
        ),
    ),
    ComicFixture(
        name = "Turned and zoomed images",
        comic = fixture(
            pageShape = PageShape.SQUARE,
            layout = Layout(Grid(rows = 2, columns = 2)),
            images = listOf(
                PanelImage(SAMPLE),
                PanelImage(SAMPLE, angleDegrees = 30f),
                PanelImage(SAMPLE, zoom = 2f),
                PanelImage(SAMPLE, centre = NormalizedPoint(0.2f, 0.2f), zoom = 2f, angleDegrees = -15f),
            ),
        ),
    ),
    ComicFixture(
        name = "Wide gutter and margin",
        comic = fixture(
            pageShape = PageShape.SQUARE,
            layout = Layout(Grid(rows = 2, columns = 2)),
            style = ComicStyle(pageMargin = 0.08f, gutter = 0.06f, borderThickness = 0.008f),
        ),
    ),
    ComicFixture(
        name = "Balloon types, one per panel",
        comic = fixture(
            pageShape = PageShape.SQUARE,
            layout = Layout(Grid(rows = 2, columns = 3)),
        ).copy(
            balloons = BalloonType.entries.mapIndexed { index, type ->
                Balloon(
                    id = index.toLong(),
                    type = type,
                    scope = BalloonScope.Panel(index),
                    centre = NormalizedPoint(0.5f, 0.45f),
                    width = 0.22f,
                    height = 0.13f,
                    tailAngleDegrees = 80f,
                    tailLength = 0.05f,
                )
            },
        ),
    ),
    ComicFixture(
        name = "Panel balloon clipped, comic balloon free",
        comic = fixture(
            pageShape = PageShape.SQUARE,
            layout = Layout(Grid(rows = 2, columns = 2)),
        ).copy(
            balloons = listOf(
                // Pushed to the panel's corner, so the panel cuts it off.
                Balloon(
                    id = 1,
                    type = BalloonType.SPEAK,
                    scope = BalloonScope.Panel(0),
                    centre = NormalizedPoint(0.9f, 0.9f),
                    width = 0.3f,
                    height = 0.18f,
                    tailLength = 0.06f,
                ),
                // Sitting over the gutter, which only a comic balloon may do.
                Balloon(
                    id = 2,
                    type = BalloonType.CAPTION,
                    scope = BalloonScope.Comic,
                    centre = NormalizedPoint(0.5f, 0.5f),
                    width = 0.42f,
                    height = 0.12f,
                    tailLength = 0f,
                ),
            ),
        ),
    ),
)

private fun fixture(
    pageShape: PageShape,
    layout: Layout,
    style: ComicStyle = ComicStyle(),
    images: List<PanelImage>? = null,
): Comic {
    val panelCount = panelShapes(layout, pageShape, style).size
    return Comic(
        pageShape = pageShape,
        style = style,
        layout = layout,
        panels = List(panelCount) { index ->
            Panel(images?.getOrNull(index) ?: PanelImage(SAMPLE))
        },
    )
}
