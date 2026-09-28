package com.ballooner.domain.comic

import com.ballooner.domain.model.BalloonType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ComicLayoutChangeTest {

    private val style = ComicStyle(pageMargin = 0f, gutter = 0f, borderThickness = 0f)

    private fun comicWith(grid: Grid, cuts: List<Cut> = emptyList()): Comic {
        val comic = Comic(sizing = PageSizing.Ratio(SQUARE_RATIO), style = style, layout = Layout(grid, cuts))
        val panelCount = panelShapes(comic.layout, comic.pageHeight, style).size
        return comic.copy(panels = List(panelCount) { Panel(PanelImage(sourceUri = "image$it")) })
    }

    private fun uris(comic: Comic) = comic.panels.map { it.image?.sourceUri }

    @Test
    fun `splitting a panel leaves the image with the first piece`() {
        val comic = comicWith(Grid(1, 1))
        val cut = Cut(NormalizedPoint(0.5f, 0f), NormalizedPoint(0.5f, 1f), CutScope.WholePage)

        val change = comic.withLayout(comic.layout.withCut(cut))

        assertEquals(listOf("image0", null), uris(change.comic))
        assertFalse(change.isDestructive)
    }

    @Test
    fun `removing a cut leaves the image with the first of the two panels`() {
        val cut = Cut(NormalizedPoint(0.5f, 0f), NormalizedPoint(0.5f, 1f), CutScope.WholePage)
        val comic = comicWith(Grid(1, 1), listOf(cut))

        val change = comic.withLayout(comic.layout.withoutCutAt(0))

        assertEquals(listOf("image0"), uris(change.comic))
        assertEquals(1, change.removedImages)
        assertTrue(change.isDestructive)
    }

    @Test
    fun `merging two panels keeps the first image and reports the other as removed`() {
        val comic = comicWith(Grid(rows = 1, columns = 2))
        val merged = comic.layout.grid.mergedFrom(listOf(Span(0, 0), Span(0, 1)))!!

        val change = comic.withLayout(Layout(merged))

        assertEquals(listOf("image0"), uris(change.comic))
        assertEquals(1, change.removedImages)
    }

    @Test
    fun `unmerging keeps the image in the first cell and empties the rest`() {
        val grid = Grid(rows = 1, columns = 2, spans = listOf(Span(0, 0, rowCount = 1, columnCount = 2)))
        val comic = comicWith(grid)

        val change = comic.withLayout(Layout(grid.unmergedAt(0, 0)))

        assertEquals(listOf("image0", null), uris(change.comic))
        assertFalse(change.isDestructive)
    }

    @Test
    fun `dragging a grid line keeps every image in place`() {
        val comic = comicWith(Grid(rows = 1, columns = 2))
        val widened = Grid(rows = 1, columns = 2, columnWeights = listOf(3f, 1f))

        val change = comic.withLayout(Layout(widened))

        assertEquals(listOf("image0", "image1"), uris(change.comic))
        assertFalse(change.isDestructive)
    }

    @Test
    fun `a panel balloon follows its panel to a new index`() {
        val comic = comicWith(Grid(rows = 1, columns = 2)).let { base ->
            base.copy(
                balloons = listOf(
                    Balloon(id = 1, type = BalloonType.SPEAK, scope = BalloonScope.Panel(1)),
                ),
            )
        }
        val cut = Cut(
            a = NormalizedPoint(0.25f, 0f),
            b = NormalizedPoint(0.25f, 1f),
            scope = CutScope.AtPoint(NormalizedPoint(0.25f, 0.5f)),
        )

        val change = comic.withLayout(comic.layout.withCut(cut))

        assertEquals(3, change.comic.panels.size)
        assertEquals(BalloonScope.Panel(2), change.comic.balloons.single().scope)
        assertEquals(0, change.removedBalloons)
    }

    @Test
    fun `a panel balloon is removed with its panel`() {
        val comic = comicWith(Grid(rows = 1, columns = 2)).let { base ->
            base.copy(
                balloons = listOf(
                    Balloon(id = 1, type = BalloonType.SPEAK, scope = BalloonScope.Panel(0)),
                    Balloon(id = 2, type = BalloonType.SPEAK, scope = BalloonScope.Panel(1)),
                ),
            )
        }
        val merged = comic.layout.grid.mergedFrom(listOf(Span(0, 0), Span(0, 1)))!!

        val change = comic.withLayout(Layout(merged))

        assertEquals(listOf(1L), change.comic.balloons.map { it.id })
        assertEquals(1, change.removedBalloons)
    }

    @Test
    fun `a comic balloon survives any layout change`() {
        val comic = comicWith(Grid(rows = 1, columns = 2)).let { base ->
            base.copy(balloons = listOf(Balloon(id = 7, type = BalloonType.CAPTION, scope = BalloonScope.Comic)))
        }
        val merged = comic.layout.grid.mergedFrom(listOf(Span(0, 0), Span(0, 1)))!!

        val change = comic.withLayout(Layout(merged))

        assertEquals(BalloonScope.Comic, change.comic.balloons.single().scope)
        assertEquals(0, change.removedBalloons)
    }

    @Test
    fun `growing the grid by index keeps every image and leaves the new panels empty`() {
        val comic = comicWith(Grid(rows = 1, columns = 4))

        val change = comic.withLayout(Layout(Grid(rows = 2, columns = 3)), PanelMatching.BY_INDEX)

        assertEquals(listOf("image0", "image1", "image2", "image3", null, null), uris(change.comic))
        assertFalse(change.isDestructive)
    }

    @Test
    fun `reshaping the grid by index keeps the same panel count and every image`() {
        val comic = comicWith(Grid(rows = 1, columns = 4))

        val change = comic.withLayout(Layout(Grid(rows = 2, columns = 2)), PanelMatching.BY_INDEX)

        assertEquals(listOf("image0", "image1", "image2", "image3"), uris(change.comic))
        assertFalse(change.isDestructive)
    }

    @Test
    fun `shrinking the grid by index drops the trailing panels and reports the loss`() {
        val comic = comicWith(Grid(rows = 1, columns = 4)).let { base ->
            base.copy(
                balloons = listOf(
                    Balloon(id = 1, type = BalloonType.SPEAK, scope = BalloonScope.Panel(0)),
                    Balloon(id = 2, type = BalloonType.SPEAK, scope = BalloonScope.Panel(3)),
                ),
            )
        }

        val change = comic.withLayout(Layout(Grid(rows = 1, columns = 2)), PanelMatching.BY_INDEX)

        assertEquals(listOf("image0", "image1"), uris(change.comic))
        assertEquals(2, change.removedImages)
        assertEquals(1, change.removedBalloons)
        assertEquals(listOf(1L), change.comic.balloons.map { it.id })
        assertTrue(change.isDestructive)
    }

    @Test
    fun `changing the page sizing keeps every panel`() {
        val comic = comicWith(Grid(rows = 2, columns = 2))

        val change = comic.copy(sizing = PageSizing.Ratio(WIDE_RATIO)).withLayout(comic.layout)

        assertEquals(listOf("image0", "image1", "image2", "image3"), uris(change.comic))
        assertFalse(change.isDestructive)
    }

    @Test
    fun `an untouched panel keeps its image placement`() {
        val placed = PanelImage("image0", NormalizedPoint(0.3f, 0.7f), zoom = 2.5f, angleDegrees = 12f)
        val comic = comicWith(Grid(rows = 1, columns = 2)).let { base ->
            base.copy(panels = listOf(Panel(placed), base.panels[1]))
        }

        val change = comic.withLayout(Layout(Grid(rows = 1, columns = 2, columnWeights = listOf(2f, 1f))))

        assertEquals(placed, change.comic.panels[0].image)
    }

    @Test
    fun `an empty comic layout change removes nothing`() {
        val comic = Comic()

        val change = comic.withLayout(Layout(Grid(rows = 2, columns = 2)), PanelMatching.BY_INDEX)

        assertEquals(4, change.comic.panels.size)
        assertNull(change.comic.panels[1].image)
        assertFalse(change.isDestructive)
    }
}
