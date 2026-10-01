package com.ballooner.domain.comic

/**
 * A point on the page in normalized coordinates, so it survives a change in the page's height.
 *
 * The page is always 1 page unit wide, so every other distance in the model is a fraction of the
 * page width and means the same thing on both axes.
 */
data class NormalizedPoint(val u: Float, val v: Float) {
    fun onPage(pageHeight: Float): PagePoint = PagePoint(u, v * pageHeight)
}

/** The same point as a fraction of the page, which is how the document stores it. */
fun PagePoint.normalized(pageHeight: Float): NormalizedPoint =
    NormalizedPoint(x, if (pageHeight > 0f) y / pageHeight else 0f)

/** Comic-level styling. Every distance is a fraction of the page width. */
data class ComicStyle(
    val gutter: Float = 0.02f,
    val borderThickness: Float = 0.004f,
    val cornerRadius: Float = 0f,
) {
    /** The gap round the outside of the page, which is the gutter so every gap matches. */
    val pageMargin: Float get() = gutter
}
