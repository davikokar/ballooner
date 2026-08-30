package com.ballooner.ui.project

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.ballooner.domain.model.Balloon
import com.ballooner.domain.model.BalloonType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BalloonTailTest {

    @Test
    fun `tailAtPoint places the tip exactly at the target point`() {
        val canvas = Size(1000f, 800f)
        val balloon = Balloon(id = 1, type = BalloonType.SPEAK)
        val target = Offset(760f, 240f)

        val tip = balloon.tailAtPoint(target, canvas).tailTip(canvas)

        assertEquals(target.x, tip.x, 0.5f)
        assertEquals(target.y, tip.y, 0.5f)
    }

    @Test
    fun `tailAtPoint clamps the tail to zero length when the target is inside the body`() {
        val canvas = Size(1000f, 800f)
        val balloon = Balloon(id = 1, type = BalloonType.SPEAK)

        val updated = balloon.tailAtPoint(balloon.bodyCenter(canvas), canvas)

        assertEquals(0f, updated.tailLength, 0.0001f)
    }

    @Test
    fun `tailWidthFromPoint round-trips the tail base handle position`() {
        val canvas = Size(1000f, 800f)
        val balloon = Balloon(id = 1, type = BalloonType.SPEAK)

        val updated = balloon.tailWidthFromPoint(balloon.tailBaseHandle(canvas), canvas)

        assertEquals(balloon.tailWidth, updated.tailWidth, 0.001f)
    }

    @Test
    fun `tail sides curve outward from their straight midpoints`() {
        val canvas = Size(1000f, 800f)
        val balloon = Balloon(id = 1, type = BalloonType.SPEAK, tailAngleDegrees = 90f)

        val tail = balloon.curvedTail(canvas)

        val centerX = balloon.bodyCenter(canvas).x
        val firstStraightMidpointX = (tail.firstBase.x + tail.tip.x) / 2f
        val secondStraightMidpointX = (tail.secondBase.x + tail.tip.x) / 2f
        assertTrue(tail.firstControl.x < firstStraightMidpointX)
        assertTrue(tail.secondControl.x > secondStraightMidpointX)
        assertEquals(centerX, (tail.firstControl.x + tail.secondControl.x) / 2f, 0.001f)
    }

    @Test
    fun `think tail starts at the balloon edge and ends at the tip`() {
        val canvas = Size(1000f, 800f)
        val balloon = Balloon(id = 1, type = BalloonType.THINK)

        val bubbles = balloon.thinkTailBubbles(canvas)

        val edge = balloon.copy(tailLength = 0f).tailTip(canvas)
        assertEquals(edge.x, bubbles.first().center.x, 0.001f)
        assertEquals(edge.y, bubbles.first().center.y, 0.001f)
        assertEquals(balloon.tailTip(canvas).x, bubbles.last().center.x, 0.001f)
        assertEquals(balloon.tailTip(canvas).y, bubbles.last().center.y, 0.001f)
    }
}
