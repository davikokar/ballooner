package com.ballooner.domain.comic

/**
 * The proportions of a comic page. The page is always 1 page unit wide, so every other distance
 * in the model is a fraction of the page width and means the same thing on both axes.
 */
enum class PageShape(val aspectRatio: Float) {
    SQUARE(1f),
    PORTRAIT(3f / 4f),
    LANDSCAPE(4f / 3f),
    STRIP(3f),
    ;

    val pageHeight: Float get() = 1f / aspectRatio
}

/** A point on the page in normalized coordinates, so it survives a change of [PageShape]. */
data class NormalizedPoint(val u: Float, val v: Float) {
    fun onPage(pageShape: PageShape): PagePoint = PagePoint(u, v * pageShape.pageHeight)
}

/** Comic-level styling. Every distance is a fraction of the page width. */
data class ComicStyle(
    val pageMargin: Float = 0.02f,
    val gutter: Float = 0.02f,
    val borderThickness: Float = 0.004f,
    val cornerRadius: Float = 0f,
)
