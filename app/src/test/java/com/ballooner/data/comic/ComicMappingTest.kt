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
import com.ballooner.domain.comic.PageSizing
import com.ballooner.domain.comic.Panel
import com.ballooner.domain.comic.PanelImage
import com.ballooner.domain.comic.PanelStyle
import com.ballooner.domain.comic.Span
import com.ballooner.domain.comic.WIDE_RATIO
import com.ballooner.domain.model.BalloonFont
import com.ballooner.domain.model.BalloonType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ComicMappingTest {

    private fun roundTrip(comic: Comic): Comic =
        comic.toParts(id = 3, createdAt = 10, updatedAt = 20).toDomain()

    @Test
    fun `an empty comic survives a round trip`() {
        val comic = Comic(name = "Untitled")

        assertEquals(comic, roundTrip(comic))
    }

    @Test
    fun `page sizing and style survive a round trip`() {
        val comic = Comic(
            name = "Styled",
            sizing = PageSizing.Ratio(WIDE_RATIO),
            style = ComicStyle(gutter = 0.03f, borderThickness = 0.01f, cornerRadius = 0.02f),
        )

        assertEquals(comic, roundTrip(comic))
    }

    @Test
    fun `a panel's own frame survives a round trip`() {
        val comic = Comic(
            layout = Layout(Grid(rows = 1, columns = 2)),
            panels = listOf(
                Panel(style = PanelStyle(borderThickness = 0.02f, cornerRadius = 0.3f)),
                Panel(),
            ),
        )

        assertEquals(comic, roundTrip(comic))
    }

    @Test
    fun `grid weights and merged spans survive a round trip`() {
        val comic = Comic(
            layout = Layout(
                Grid(
                    rows = 3,
                    columns = 2,
                    rowWeights = listOf(1f, 2.5f, 0.5f),
                    columnWeights = listOf(3f, 1f),
                    spans = listOf(Span(0, 0, rowCount = 1, columnCount = 2), Span(1, 0, rowCount = 2, columnCount = 1)),
                ),
            ),
            panels = List(3) { Panel() },
        )

        assertEquals(comic, roundTrip(comic))
    }

    @Test
    fun `cuts keep their order and their scope`() {
        val pageCut = Cut(NormalizedPoint(0f, 0.2f), NormalizedPoint(1f, 0.8f), CutScope.WholePage)
        val panelCut = Cut(
            a = NormalizedPoint(0.5f, 0f),
            b = NormalizedPoint(0.5f, 1f),
            scope = CutScope.AtPoint(NormalizedPoint(0.25f, 0.75f)),
        )
        val comic = Comic(layout = Layout(Grid(1, 1), listOf(pageCut, panelCut)), panels = List(3) { Panel() })

        val restored = roundTrip(comic)

        assertEquals(listOf(pageCut, panelCut), restored.layout.cuts)
    }

    @Test
    fun `panel images keep their placement`() {
        val comic = Comic(
            panels = listOf(
                Panel(PanelImage("file://a", NormalizedPoint(0.3f, 0.7f), zoom = 2.25f, angleDegrees = -33.5f)),
                Panel(),
            ),
        )

        val restored = roundTrip(comic)

        assertEquals(comic.panels, restored.panels)
        assertNull(restored.panels[1].image)
    }

    @Test
    fun `balloons keep both scopes and their drawing order`() {
        val comic = Comic(
            balloons = listOf(
                Balloon(id = 1, type = BalloonType.SPEAK, scope = BalloonScope.Panel(2), text = "first"),
                Balloon(id = 2, type = BalloonType.CAPTION, scope = BalloonScope.Comic, text = "second"),
                Balloon(id = 3, type = BalloonType.THINK, scope = BalloonScope.Panel(0), text = "third"),
            ),
        )

        val restored = roundTrip(comic)

        assertEquals(comic.balloons, restored.balloons)
        assertEquals(listOf("first", "second", "third"), restored.balloons.map { it.text })
    }

    @Test
    fun `every balloon property survives a round trip`() {
        val balloon = Balloon(
            id = 9,
            type = BalloonType.YELL,
            scope = BalloonScope.Panel(1),
            text = "Ouch!",
            centre = NormalizedPoint(0.2f, 0.8f),
            width = 0.33f,
            height = 0.17f,
            tailAngleDegrees = 215f,
            tailLength = 0.09f,
            tailWidth = 0.4f,
            cornerRoundness = 0.25f,
            fontSize = 0.06f,
            autoSize = true,
            font = BalloonFont.COMIC_SANS_MS,
        )

        assertEquals(balloon, roundTrip(Comic(balloons = listOf(balloon))).balloons.single())
    }

    @Test
    fun `the comic id is written onto every part`() {
        val comic = Comic(
            layout = Layout(
                grid = Grid(rows = 2, columns = 2, spans = listOf(Span(0, 0, 1, 2))),
                cuts = listOf(Cut(NormalizedPoint(0f, 0f), NormalizedPoint(1f, 1f), CutScope.WholePage)),
            ),
            panels = List(3) { Panel() },
            balloons = listOf(Balloon(id = 1, type = BalloonType.SPEAK, scope = BalloonScope.Comic)),
        )

        val parts = comic.toParts(id = 42, createdAt = 1, updatedAt = 2)

        assertEquals(listOf(42L), parts.spans.map { it.comicId }.distinct())
        assertEquals(listOf(42L), parts.cuts.map { it.comicId }.distinct())
        assertEquals(listOf(42L), parts.panels.map { it.comicId }.distinct())
        assertEquals(listOf(42L), parts.balloons.map { it.comicId }.distinct())
    }

    @Test
    fun `timestamps are carried onto the comic row`() {
        val parts = Comic(name = "Dated").toParts(id = 1, createdAt = 100, updatedAt = 200)

        assertEquals(100L, parts.comic.createdAt)
        assertEquals(200L, parts.comic.updatedAt)
        assertEquals("Dated", parts.comic.name)
    }

    @Test
    fun `parts stored out of order are restored in order`() {
        val comic = Comic(
            panels = listOf(Panel(PanelImage("a")), Panel(PanelImage("b")), Panel(PanelImage("c"))),
        )
        val parts = comic.toParts(id = 1, createdAt = 0, updatedAt = 0)

        val shuffled = parts.copy(panels = parts.panels.reversed())

        assertEquals(listOf("a", "b", "c"), shuffled.toDomain().panels.map { it.image?.sourceUri })
    }

    @Test
    fun `a comic with no stored page ratio takes its shape from its image`() {
        val parts = Comic(name = "Odd").toParts(id = 1, createdAt = 0, updatedAt = 0)

        val unset = parts.copy(comic = parts.comic.copy(pageRatio = null))

        assertEquals(PageSizing.FromImage, unset.toDomain().sizing)
    }

    @Test
    fun `weights convert through a plain comma separated column`() {
        val converter = FloatListConverter()

        assertEquals("1.0,2.5", converter.fromFloatList(listOf(1f, 2.5f)))
        assertEquals(listOf(1f, 2.5f), converter.toFloatList("1.0,2.5"))
        assertEquals(emptyList<Float>(), converter.toFloatList(""))
    }
}
