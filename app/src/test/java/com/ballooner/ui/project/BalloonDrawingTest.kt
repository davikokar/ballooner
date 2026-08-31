package com.ballooner.ui.project

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.ballooner.domain.model.Balloon
import com.ballooner.domain.model.BalloonType
import com.ballooner.domain.model.ImagePosition
import com.ballooner.domain.model.RectFraction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun `control scale cancels rotation fitting scale`() {
        assertEquals(0.25f, fixedControlScale(contentScale = 4f), 0.001f)
        assertEquals(1f, fixedControlScale(contentScale = 0f), 0.001f)
    }

    @Test
    fun `image focus uses the selected panel and falls back to the first panel`() {
        val first = RectFraction(0f, 0f, 0.5f, 1f)
        val second = RectFraction(0.5f, 0f, 0.5f, 1f)

        assertEquals(second, imageFocusTarget(listOf(first, second), second, focusedPanel = null))
        assertEquals(first, imageFocusTarget(listOf(first, second), selectedPanel = null, focusedPanel = null))
    }

    @Test
    fun `image focus toggle restores the multiimage view`() {
        val focused = RectFraction(0f, 0f, 0.5f, 1f)

        assertNull(imageFocusTarget(listOf(focused), selectedPanel = null, focusedPanel = focused))
    }

    @Test
    fun `focused panel maps exactly onto the viewport`() {
        val panel = RectFraction(left = 0.5f, top = 0.25f, width = 0.25f, height = 0.5f)

        val layout = panel.focusLayout(viewportWidth = 400f, viewportHeight = 600f)

        assertEquals(1600f, layout.contentWidth, 0.001f)
        assertEquals(1200f, layout.contentHeight, 0.001f)
        assertEquals(0f, layout.offsetX + panel.left * layout.contentWidth, 0.001f)
        assertEquals(0f, layout.offsetY + panel.top * layout.contentHeight, 0.001f)
        assertEquals(400f, layout.offsetX + (panel.left + panel.width) * layout.contentWidth, 0.001f)
        assertEquals(600f, layout.offsetY + (panel.top + panel.height) * layout.contentHeight, 0.001f)
    }

    @Test
    fun `focused image navigation exposes only adjacent panels`() {
        val topLeft = RectFraction(0f, 0f, 0.5f, 0.5f)
        val topRight = RectFraction(0.5f, 0f, 0.5f, 0.5f)
        val bottomLeft = RectFraction(0f, 0.5f, 0.5f, 0.5f)

        val adjacent = adjacentPanels(listOf(topLeft, topRight, bottomLeft), topLeft)

        assertEquals(topRight, adjacent[ImagePosition.RIGHT])
        assertEquals(bottomLeft, adjacent[ImagePosition.BOTTOM])
        assertFalse(ImagePosition.LEFT in adjacent)
        assertFalse(ImagePosition.TOP in adjacent)
    }

    @Test
    fun `focus navigation offsets center arrows on image edges`() {
        assertEquals((-15).dp, focusNavigationOffset(ImagePosition.LEFT).x)
        assertEquals(15.dp, focusNavigationOffset(ImagePosition.RIGHT).x)
        assertEquals((-15).dp, focusNavigationOffset(ImagePosition.TOP).y)
        assertEquals(15.dp, focusNavigationOffset(ImagePosition.BOTTOM).y)
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
