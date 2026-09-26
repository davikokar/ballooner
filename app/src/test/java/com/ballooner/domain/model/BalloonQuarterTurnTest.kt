package com.ballooner.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BalloonQuarterTurnTest {

    @Test
    fun `a balloon on a rotated panel orbits the panel centre by a quarter turn`() {
        val panel = RectFraction(0.2f, 0.1f, 0.2f, 0.6f)
        val turned = quarterTurnedPanel(panel)
        // Horizontally centred, a quarter of the panel's height above its centre. A clockwise turn
        // must land it vertically centred, a quarter of the turned panel's width right of centre.
        val balloon = balloonAt(centerX = 0.3f, centerY = 0.25f)

        val remapped = balloon.remappedByQuarterTurns(from = panel, to = turned, quarterTurns = 1)

        // Turned rect is (0.2, 0.1, 0.6, 0.2), so its centre is (0.5, 0.2) and a quarter of its
        // width is 0.15.
        assertEquals(0.65f, remapped.centerX, 0.0001f)
        assertEquals(0.2f, remapped.centerY, 0.0001f)
    }

    @Test
    fun `a balloon tail advances ninety degrees with its panel`() {
        val panel = RectFraction(0.2f, 0.1f, 0.2f, 0.6f)
        val balloon = balloonAt(centerX = 0.3f, centerY = 0.25f).copy(tailAngleDegrees = 45f)

        val remapped = balloon.remappedByQuarterTurns(
            from = panel,
            to = quarterTurnedPanel(panel),
            quarterTurns = 1,
        )

        assertEquals(135f, remapped.tailAngleDegrees, 0.0001f)
    }

    @Test
    fun `a balloon keeps its width and height through a panel rotation`() {
        val panel = RectFraction(0.2f, 0.1f, 0.2f, 0.6f)
        val balloon = balloonAt(centerX = 0.3f, centerY = 0.25f).copy(width = 0.18f, height = 0.09f)

        val remapped = balloon.remappedByQuarterTurns(
            from = panel,
            to = quarterTurnedPanel(panel),
            quarterTurns = 1,
        )

        assertEquals(0.18f, remapped.width, 0.0001f)
        assertEquals(0.09f, remapped.height, 0.0001f)
    }

    @Test
    fun `rotating a balloon four times returns it to its original values`() {
        val panels = generateSequence(RectFraction(0.2f, 0.1f, 0.2f, 0.6f), ::quarterTurnedPanel)
            .take(5)
            .toList()
        val balloon = balloonAt(centerX = 0.3f, centerY = 0.25f).copy(tailAngleDegrees = 300f)

        val remapped = panels.zipWithNext().fold(balloon) { turning, (from, to) ->
            turning.remappedByQuarterTurns(from = from, to = to, quarterTurns = 1)
        }

        assertEquals(balloon.centerX, remapped.centerX, 0.0001f)
        assertEquals(balloon.centerY, remapped.centerY, 0.0001f)
        assertEquals(balloon.tailAngleDegrees, remapped.tailAngleDegrees, 0.0001f)
    }

    private fun balloonAt(centerX: Float, centerY: Float) = Balloon(
        id = 1L,
        type = BalloonType.SPEAK,
        centerX = centerX,
        centerY = centerY,
    )
}
