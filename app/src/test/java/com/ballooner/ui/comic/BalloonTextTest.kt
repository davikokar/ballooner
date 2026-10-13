package com.ballooner.ui.comic

import com.ballooner.domain.comic.Balloon
import com.ballooner.domain.comic.BalloonScope
import com.ballooner.domain.model.BalloonType
import org.junit.Assert.assertEquals
import org.junit.Test

class BalloonTextTest {

    @Test
    fun `a balloon sizing its own text keeps the usual room`() {
        val balloon = balloon(BalloonType.SPEAK, autoSize = true)

        assertEquals(TEXT_INSET, textInset(balloon), TOLERANCE)
    }

    @Test
    fun `a yell sizing its own text is held further in`() {
        val balloon = balloon(BalloonType.YELL, autoSize = true)

        assertEquals(BROKEN_EDGE_TEXT_INSET, textInset(balloon), TOLERANCE)
    }

    @Test
    fun `a thought sizing its own text is held further in`() {
        val balloon = balloon(BalloonType.THINK, autoSize = true)

        assertEquals(BROKEN_EDGE_TEXT_INSET, textInset(balloon), TOLERANCE)
    }

    @Test
    fun `a yell sized by hand keeps the room it always had`() {
        val balloon = balloon(BalloonType.YELL, autoSize = false)

        assertEquals(TEXT_INSET, textInset(balloon), TOLERANCE)
    }

    @Test
    fun `a thought sized by hand keeps the room it always had`() {
        val balloon = balloon(BalloonType.THINK, autoSize = false)

        assertEquals(TEXT_INSET, textInset(balloon), TOLERANCE)
    }

    private fun balloon(type: BalloonType, autoSize: Boolean) = Balloon(
        id = 1,
        type = type,
        scope = BalloonScope.Panel(0),
        text = "HELLO",
        autoSize = autoSize,
    )
}

private const val TOLERANCE = 1e-6f
