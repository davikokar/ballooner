package com.ballooner.ui.project

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.ballooner.domain.model.Balloon
import com.ballooner.domain.model.BalloonType
import com.ballooner.domain.model.RectFraction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BalloonDrawingTest {

    @Test
    fun `balloon clip bounds preserve the panel border`() {
        val canvas = Size(1000f, 800f)
        val panel = RectFraction(left = 0.25f, top = 0.1f, width = 0.5f, height = 0.4f)

        val bounds = panel.balloonClipBounds(canvas)

        assertEquals(253f, bounds.left, 0.001f)
        assertEquals(83f, bounds.top, 0.001f)
        assertEquals(747f, bounds.right, 0.001f)
        assertEquals(397f, bounds.bottom, 0.001f)
    }

    @Test
    fun `control scale cancels the editor content zoom`() {
        assertEquals(0.25f, fixedControlScale(contentScale = 4f), 0.001f)
        assertEquals(1f, fixedControlScale(contentScale = 0f), 0.001f)
    }

    @Test
    fun `containsPoint treats a caption as a rectangle, including its corners`() {
        val canvas = Size(1000f, 800f)
        val balloon = Balloon(id = 1, type = BalloonType.CAPTION, width = 0.4f, height = 0.2f)
        val g = balloon
        val halfX = g.width * canvas.width / 2f
        val halfY = g.height * canvas.height / 2f
        val corner = Offset(
            g.centerX * canvas.width + halfX - 1f,
            g.centerY * canvas.height + halfY - 1f,
        )

        assertTrue(balloon.containsPoint(corner, canvas))
    }

    @Test
    fun `containsPoint rejects points outside the caption rectangle`() {
        val canvas = Size(1000f, 800f)
        val balloon = Balloon(id = 1, type = BalloonType.CAPTION, width = 0.4f, height = 0.2f)

        val outside = Offset(
            balloon.centerX * canvas.width + balloon.width * canvas.width,
            balloon.centerY * canvas.height,
        )

        assertFalse(balloon.containsPoint(outside, canvas))
    }
}
