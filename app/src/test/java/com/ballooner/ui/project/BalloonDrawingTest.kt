package com.ballooner.ui.project

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.ballooner.domain.model.Balloon
import com.ballooner.domain.model.BalloonType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BalloonDrawingTest {

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
