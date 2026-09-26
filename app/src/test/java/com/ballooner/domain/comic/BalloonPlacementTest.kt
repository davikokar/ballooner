package com.ballooner.domain.comic

import com.ballooner.domain.model.BalloonType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BalloonPlacementTest {

    private val panel = PageRect(0.5f, 0.25f, 0.5f, 0.25f)
    private val pageHeight = 1.5f

    private fun panelBalloon(u: Float, v: Float) = Balloon(
        id = 1,
        type = BalloonType.SPEAK,
        scope = BalloonScope.Panel(0),
        centre = NormalizedPoint(u, v),
    )

    private fun comicBalloon(u: Float, v: Float) = Balloon(
        id = 2,
        type = BalloonType.CAPTION,
        scope = BalloonScope.Comic,
        centre = NormalizedPoint(u, v),
    )

    @Test
    fun `a panel balloon is placed inside its panel`() {
        val onPage = panelBalloon(0.5f, 0.5f).centreOnPage(panel, pageHeight)

        assertEquals(0.75f, onPage.x, TOLERANCE)
        assertEquals(0.375f, onPage.y, TOLERANCE)
    }

    @Test
    fun `a panel balloon at the corner of its panel lands on that corner`() {
        val onPage = panelBalloon(0f, 0f).centreOnPage(panel, pageHeight)

        assertEquals(panel.left, onPage.x, TOLERANCE)
        assertEquals(panel.top, onPage.y, TOLERANCE)
    }

    @Test
    fun `a comic balloon is placed on the page and ignores panels`() {
        val onPage = comicBalloon(0.25f, 0.5f).centreOnPage(panel, pageHeight)

        assertEquals(0.25f, onPage.x, TOLERANCE)
        assertEquals(0.75f, onPage.y, TOLERANCE)
    }

    @Test
    fun `a panel balloon keeps its place when its panel is reshaped`() {
        val balloon = panelBalloon(0.25f, 0.75f)
        val wider = PageRect(0.5f, 0.25f, 1f, 0.5f)

        val onPage = balloon.centreOnPage(wider, pageHeight)

        // Still a quarter across and three quarters down its panel, whatever size that is.
        assertEquals(0.75f, onPage.x, TOLERANCE)
        assertEquals(0.625f, onPage.y, TOLERANCE)
    }

    @Test
    fun `moving a panel balloon on the page is recorded relative to its panel`() {
        val moved = panelBalloon(0.5f, 0.5f)
            .withCentreOnPage(PagePoint(1f, 0.5f), panel, pageHeight)

        assertEquals(1f, moved.centre.u, TOLERANCE)
        assertEquals(1f, moved.centre.v, TOLERANCE)
    }

    @Test
    fun `moving a comic balloon is recorded against the page`() {
        val moved = comicBalloon(0.5f, 0.5f)
            .withCentreOnPage(PagePoint(0.25f, 0.75f), panel, pageHeight)

        assertEquals(0.25f, moved.centre.u, TOLERANCE)
        assertEquals(0.5f, moved.centre.v, TOLERANCE)
    }

    @Test
    fun `moving is the exact reverse of placing`() {
        val balloon = panelBalloon(0.3f, 0.8f)
        val onPage = balloon.centreOnPage(panel, pageHeight)

        val returned = balloon.withCentreOnPage(onPage, panel, pageHeight)

        assertEquals(balloon.centre.u, returned.centre.u, TOLERANCE)
        assertEquals(balloon.centre.v, returned.centre.v, TOLERANCE)
    }

    @Test
    fun `making a panel balloon into a comic balloon does not move it`() {
        val balloon = panelBalloon(0.25f, 0.75f)
        val before = balloon.centreOnPage(panel, pageHeight)

        val converted = balloon.withScope(BalloonScope.Comic, panel, null, pageHeight)
        val after = converted.centreOnPage(null, pageHeight)

        assertEquals(BalloonScope.Comic, converted.scope)
        assertEquals(before.x, after.x, TOLERANCE)
        assertEquals(before.y, after.y, TOLERANCE)
    }

    @Test
    fun `making a comic balloon into a panel balloon does not move it`() {
        val balloon = comicBalloon(0.6f, 0.3f)
        val before = balloon.centreOnPage(null, pageHeight)

        val converted = balloon.withScope(BalloonScope.Panel(0), null, panel, pageHeight)
        val after = converted.centreOnPage(panel, pageHeight)

        assertEquals(BalloonScope.Panel(0), converted.scope)
        assertEquals(before.x, after.x, TOLERANCE)
        assertEquals(before.y, after.y, TOLERANCE)
    }

    @Test
    fun `changing to the scope it already has leaves the balloon alone`() {
        val balloon = panelBalloon(0.25f, 0.75f)

        assertEquals(balloon, balloon.withScope(BalloonScope.Panel(0), panel, panel, pageHeight))
    }

    @Test
    fun `a panel balloon knows its panel and a comic balloon does not`() {
        assertEquals(0, panelBalloon(0f, 0f).panelIndex)
        assertNull(comicBalloon(0f, 0f).panelIndex)
    }

    @Test
    fun `comic balloons are drawn over panel balloons, each in the order they were added`() {
        val balloons = listOf(
            comicBalloon(0f, 0f).copy(id = 1),
            panelBalloon(0f, 0f).copy(id = 2),
            comicBalloon(0f, 0f).copy(id = 3),
            panelBalloon(0f, 0f).copy(id = 4),
        )

        assertEquals(listOf(2L, 4L, 1L, 3L), balloons.inDrawingOrder().map { it.id })
    }

    @Test
    fun `a panel balloon whose panel is missing falls back to the page`() {
        val onPage = panelBalloon(0.25f, 0.5f).centreOnPage(null, pageHeight)

        assertEquals(0.25f, onPage.x, TOLERANCE)
        assertEquals(0.75f, onPage.y, TOLERANCE)
    }
}
