package com.ballooner.ui.comiceditor

import com.ballooner.domain.comic.SQUARE_RATIO
import com.ballooner.domain.comic.TALL_RATIO
import com.ballooner.domain.comic.WIDE_RATIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PanelUnitsTest {

    @Test
    fun `a square is one unit by one`() {
        assertEquals(1 to 1, unitsFor(SQUARE_RATIO))
    }

    @Test
    fun `the upright preset comes back as two by three`() {
        assertEquals(2 to 3, unitsFor(TALL_RATIO))
    }

    @Test
    fun `the sideways preset comes back as three by two`() {
        assertEquals(3 to 2, unitsFor(WIDE_RATIO))
    }

    @Test
    fun `a ratio the sliders could make comes back as the sides that made it`() {
        assertEquals(16 to 9, unitsFor(16f / 9f))
    }

    @Test
    fun `the smallest pair wins when several describe the same shape`() {
        // 2:1, 4:2, 8:4 and 16:8 are all the same shape; the simplest is the useful one.
        assertEquals(2 to 1, unitsFor(2f))
    }

    @Test
    fun `a ratio no pair can reach lands on the closest one`() {
        val (width, height) = unitsFor(0.4321f)

        assertTrue(width in MIN_UNITS..MAX_UNITS)
        assertTrue(height in MIN_UNITS..MAX_UNITS)
        assertTrue(abs(width.toFloat() / height - 0.4321f) < 0.02f)
    }

    @Test
    fun `every pair the sliders can make survives a round trip`() {
        val lost = buildList {
            for (height in MIN_UNITS..MAX_UNITS) {
                for (width in MIN_UNITS..MAX_UNITS) {
                    val recovered = unitsFor(width.toFloat() / height)
                    val sameShape =
                        recovered.first.toFloat() / recovered.second == width.toFloat() / height
                    if (!sameShape) add("$width:$height -> $recovered")
                }
            }
        }

        assertEquals(emptyList<String>(), lost)
    }
}
