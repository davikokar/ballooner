package com.ballooner.domain.comic

import com.ballooner.domain.model.BalloonType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun `a balloon added to a small panel is shrunk to fit inside it`() {
        val small = PageRect(0f, 0f, 0.2f, 0.15f)

        val balloon = newBalloon(1, BalloonType.SPEAK, BalloonScope.Panel(0), small)

        assertTrue("too wide", balloon.width <= small.width)
        assertTrue("too tall", balloon.height + balloon.tailLength <= small.height)
        assertTrue("the text was left behind", balloon.fontSize < Balloon(type = BalloonType.SPEAK, scope = BalloonScope.Comic).fontSize)
    }

    @Test
    fun `a balloon added to a panel with room to spare is left exactly as it is`() {
        val roomy = PageRect(0f, 0f, 1f, 1f)

        val balloon = newBalloon(1, BalloonType.SPEAK, BalloonScope.Panel(0), roomy)

        assertEquals(newBalloon(1, BalloonType.SPEAK, BalloonScope.Panel(0)), balloon)
    }

    @Test
    fun `the tail width handle sits where the tail leaves the body`() {
        // Its tail points straight down, so the handle is level with the bottom of the body.
        val balloon = comicBalloon(0.5f, 0.5f)

        val handle = balloon.tailWidthHandle(panel = null, pageHeight = pageHeight)

        assertEquals(0.5f * pageHeight + balloon.height / 2f, handle.y, TOLERANCE)
        assertTrue("the handle should sit off to one side", handle.x < 0.5f)
    }

    @Test
    fun `dragging the width handle to where it already is leaves the tail as wide as it was`() {
        val balloon = comicBalloon(0.5f, 0.5f).copy(tailWidth = 0.6f)

        val handle = balloon.tailWidthHandle(panel = null, pageHeight = pageHeight)
        val after = balloon.withTailWidthAt(handle, panel = null, pageHeight = pageHeight)

        assertEquals(balloon.tailWidth, after.tailWidth, TOLERANCE)
    }

    @Test
    fun `the edit handle sits on the top left corner`() {
        val balloon = comicBalloon(0.5f, 0.4f)

        val handle = balloon.editHandle(panel = null, pageHeight = pageHeight)

        assertEquals(0.5f - balloon.width / 2f, handle.x, TOLERANCE)
        assertEquals(0.4f * pageHeight - balloon.height / 2f, handle.y, TOLERANCE)
    }

    @Test
    fun `the move handle rides the middle of the top edge`() {
        val balloon = comicBalloon(0.5f, 0.4f)

        val handle = balloon.moveHandle(panel = null, pageHeight = pageHeight)

        assertEquals(0.5f, handle.x, TOLERANCE)
        assertEquals(0.4f * pageHeight - balloon.height / 2f, handle.y, TOLERANCE)
    }

    @Test
    fun `the delete handle sits on the top right corner`() {
        val balloon = comicBalloon(0.5f, 0.4f)

        val handle = balloon.deleteHandle(panel = null, pageHeight = pageHeight)

        assertEquals(0.5f + balloon.width / 2f, handle.x, TOLERANCE)
        assertEquals(0.4f * pageHeight - balloon.height / 2f, handle.y, TOLERANCE)
    }

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
