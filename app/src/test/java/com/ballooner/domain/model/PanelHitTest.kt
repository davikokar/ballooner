package com.ballooner.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PanelHitTest {

    @Test
    fun `pressing the fourth panel in a grid selects the fourth panel`() {
        val panels = listOf(
            RectFraction(0f, 0f, 0.48f, 0.48f),
            RectFraction(0.52f, 0f, 0.48f, 0.48f),
            RectFraction(0f, 0.52f, 0.48f, 0.48f),
            RectFraction(0.52f, 0.52f, 0.48f, 0.48f),
        )

        assertEquals(panels[3], panels.panelAt(x = 0.75f, y = 0.75f))
    }

    @Test
    fun `overlapping bounds select the panel whose center is nearest the press`() {
        val expected = RectFraction(0.5f, 0.5f, 0.5f, 0.5f)
        val panels = listOf(
            RectFraction(0f, 0f, 0.8f, 0.8f),
            expected,
            RectFraction(0.45f, 0.45f, 0.4f, 0.4f),
        )

        assertEquals(expected, panels.panelAt(x = 0.74f, y = 0.74f))
    }

    @Test
    fun `pressing a gap selects no panel`() {
        val panels = listOf(
            RectFraction(0f, 0f, 0.48f, 1f),
            RectFraction(0.52f, 0f, 0.48f, 1f),
        )

        assertNull(panels.panelAt(x = 0.5f, y = 0.5f))
    }
}