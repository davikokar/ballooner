package com.ballooner.ui.project

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
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

    private fun assertRectEquals(expected: RectFraction, actual: RectFraction) {
        assertEquals(expected.left, actual.left, 0.0001f)
        assertEquals(expected.top, actual.top, 0.0001f)
        assertEquals(expected.width, actual.width, 0.0001f)
        assertEquals(expected.height, actual.height, 0.0001f)
    }

    @Test
    fun `final drag offset commits the magnetic preview position`() {
        val anchor = RectFraction(0f, 0f, 0.4f, 0.4f)
        val moving = RectFraction(0.6f, 0f, 0.4f, 0.4f)

        val destination = magneticDragDestination(
            panels = listOf(anchor, moving),
            moving = moving,
            dragOffset = Offset(-175f, 10f),
            displaySize = Size(1000f, 1000f),
            imageSize = IntSize(1000, 1000),
            snapThresholdDisplayPx = 28f,
        )

        assertEquals(0.408f, destination.left, 0.0001f)
        assertEquals(0f, destination.top, 0.0001f)
    }

    @Test
    fun `resize drag commits magnetically aligned proportional dimensions`() {
        val moving = RectFraction(0f, 0f, 0.3f, 0.3f)
        val rightNeighbor = RectFraction(0.5f, 0f, 0.2f, 0.3f)
        val bottomNeighbor = RectFraction(0f, 0.6f, 0.3f, 0.2f)

        val destination = magneticResizeDestination(
            panels = listOf(moving, rightNeighbor, bottomNeighbor),
            moving = moving,
            dragOffset = Offset(190f, 190f),
            displaySize = Size(1000f, 1000f),
            imageSize = IntSize(1000, 1000),
            snapThresholdDisplayPx = 20f,
        )

        assertEquals(0.5f, destination.width, 0.0001f)
        assertEquals(0.5f, destination.height, 0.0001f)
    }

    @Test
    fun `crop handle drag creates a proportional source window`() {
        val panel = RectFraction(0f, 0f, 0.5f, 0.5f)

        val source = cropSourceAfterHandleDrag(
            panel = panel,
            source = panel,
            dragOffset = Offset(100f, -100f),
            displaySize = Size(1000f, 1000f),
        )

        assertRectEquals(RectFraction(0.1f, 0f, 0.4f, 0.4f), source)
    }

    @Test
    fun `dragging cropped picture pans source within original panel`() {
        val panel = RectFraction(0f, 0f, 0.5f, 0.5f)
        val source = RectFraction(0.1f, 0f, 0.4f, 0.4f)

        val panned = panCropSource(
            panel = panel,
            source = source,
            dragOffset = Offset(200f, -200f),
            displaySize = Size(1000f, 1000f),
        )

        assertRectEquals(RectFraction(0f, 0.1f, 0.4f, 0.4f), panned)
    }

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
    fun `balloon text dimensions scale with focused image layer`() {
        val focusedContentWidth = 800f
        val normalContentWidth = 400f

        val scale = balloonTextScale(focusedContentWidth, normalContentWidth)

        assertEquals(2f, scale, 0.001f)
        assertEquals(48f, scaledBalloonTextDimension(24f, scale), 0.001f)
        assertEquals(36f, scaledBalloonTextDimension(18f, scale), 0.001f)
    }

    @Test
    fun `balloon text area follows the body shape`() {
        val squareSpeak = balloonTextAreaPx(
            type = BalloonType.SPEAK,
            cornerRoundness = 0f,
            boxWidth = 200f,
            boxHeight = 100f,
            contentScale = 1f,
        )
        val roundSpeak = balloonTextAreaPx(
            type = BalloonType.SPEAK,
            cornerRoundness = 1f,
            boxWidth = 200f,
            boxHeight = 100f,
            contentScale = 1f,
        )
        val yell = balloonTextAreaPx(
            type = BalloonType.YELL,
            cornerRoundness = 0f,
            boxWidth = 200f,
            boxHeight = 100f,
            contentScale = 1f,
        )
        val think = balloonTextAreaPx(
            type = BalloonType.THINK,
            cornerRoundness = 0f,
            boxWidth = 200f,
            boxHeight = 100f,
            contentScale = 1f,
        )

        assertEquals(194f, squareSpeak.width, 0.001f)
        assertEquals(94f, squareSpeak.height, 0.001f)
        assertTrue(roundSpeak.width < squareSpeak.width)
        assertTrue(roundSpeak.height < squareSpeak.height)
        assertTrue(yell.width < think.width)
        assertTrue(think.width < roundSpeak.width)
    }

    @Test
    fun `quarter turn swaps focused panel layout dimensions`() {
        val panel = RectFraction(left = 0.5f, top = 0f, width = 0.25f, height = 1f)

        val layout = panel.focusLayout(viewportWidth = 600f, viewportHeight = 300f, quarterTurned = true)

        assertEquals(1200f, layout.contentWidth, 0.001f)
        assertEquals(600f, layout.contentHeight, 0.001f)
        assertEquals(300f, layout.offsetX + (panel.left + panel.width / 2f) * layout.contentWidth, 0.001f)
        assertEquals(150f, layout.offsetY + (panel.top + panel.height / 2f) * layout.contentHeight, 0.001f)
    }

    @Test
    fun `rotation targets the only focused or selected image`() {
        val first = RectFraction(0f, 0f, 0.5f, 1f)
        val second = RectFraction(0.5f, 0f, 0.5f, 1f)

        assertEquals(first, rotationTarget(listOf(first), selectedPanel = null, focusedPanel = null))
        assertNull(rotationTarget(listOf(first, second), selectedPanel = null, focusedPanel = null))
        assertEquals(second, rotationTarget(listOf(first, second), selectedPanel = second, focusedPanel = null))
        assertEquals(first, rotationTarget(listOf(first, second), selectedPanel = second, focusedPanel = first))
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
    fun `focused navigation uses coordinates for irregular panel positions`() {
        val focused = RectFraction(0.4f, 0.4f, 0.2f, 0.2f)
        val nearRight = RectFraction(0.65f, 0.46f, 0.12f, 0.18f)
        val farRight = RectFraction(0.85f, 0.1f, 0.1f, 0.1f)
        val above = RectFraction(0.32f, 0.05f, 0.15f, 0.2f)

        val adjacent = adjacentPanels(listOf(focused, nearRight, farRight, above), focused)

        assertEquals(nearRight, adjacent[ImagePosition.RIGHT])
        assertEquals(above, adjacent[ImagePosition.TOP])
    }

    @Test
    fun `add panel handles appear only for the tapped panel`() {
        val left = RectFraction(0f, 0f, 0.45f, 1f)
        val right = RectFraction(0.55f, 0f, 0.45f, 1f)
        val panels = listOf(left, right)

        val placements = addPanelPlacements(panels, focusedPanel = null, tappedPanel = left)

        assertTrue(placements.isNotEmpty())
        assertTrue(placements.all { it.anchor == left })
        assertTrue(addPanelPlacements(panels, focusedPanel = null, tappedPanel = null).isEmpty())
        assertTrue(addPanelPlacements(panels, focusedPanel = left, tappedPanel = left).isEmpty())
    }

    @Test
    fun `focus navigation offsets center arrows on image edges`() {
        assertEquals((-15).dp, focusNavigationOffset(ImagePosition.LEFT).x)
        assertEquals(15.dp, focusNavigationOffset(ImagePosition.RIGHT).x)
        assertEquals((-15).dp, focusNavigationOffset(ImagePosition.TOP).y)
        assertEquals(15.dp, focusNavigationOffset(ImagePosition.BOTTOM).y)
    }

    @Test
    fun `handles outside their image are dimmed but remain visible`() {
        val imageBounds = androidx.compose.ui.geometry.Rect(100f, 100f, 300f, 300f)

        assertEquals(1f, handleAlpha(Offset(200f, 200f), imageBounds), 0.001f)
        assertEquals(0.55f, handleAlpha(Offset(80f, 200f), imageBounds), 0.001f)
        assertEquals(Offset(100f, 200f), visibleHandleCenter(Offset(80f, 200f), imageBounds))
    }

    @Test
    fun `balloon outside all images keeps the nearest image as owner`() {
        val left = RectFraction(0f, 0f, 0.45f, 1f)
        val right = RectFraction(0.55f, 0f, 0.45f, 1f)

        assertEquals(left, listOf(left, right).ownerPanel(x = -0.2f, y = 0.5f))
        assertEquals(right, listOf(left, right).ownerPanel(x = 1.2f, y = 0.5f))
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
