package com.ballooner.domain.comic

/** A panel exactly as wide as it is tall. */
const val SQUARE_RATIO = 1f

/** An upright panel, two wide by three tall. */
const val TALL_RATIO = 2f / 3f

/** A panel on its side, three wide by two tall. */
const val WIDE_RATIO = 3f / 2f

/**
 * The widest and narrowest a page may become, so a stray ratio cannot collapse the comic.
 *
 * Wide enough for a real strip: six panels of 3:2 side by side is a page nine times wider than it
 * is tall, and the same strip stood on end is nine times taller than it is wide.
 */
const val MIN_PAGE_HEIGHT = 0.05f
const val MAX_PAGE_HEIGHT = 20f

/**
 * What gives the page its height.
 *
 * The page is always one unit wide, so its height is the only measurement the document has to
 * supply. Nothing else can supply it: grid weights divide whatever height they are given, and cuts
 * are stored in normalized coordinates, so both describe proportions *between* panels and never an
 * absolute size. The page therefore takes its height from the shape of one panel — the reference
 * panel, which is the first panel of the grid — and the page shape stops being a thing the user
 * picks at all.
 */
sealed interface PageSizing {

    /** Holds the reference panel at [value], its width divided by its height. */
    data class Ratio(val value: Float) : PageSizing

    /** Holds the reference panel at the shape of the image placed in it. */
    data object FromImage : PageSizing
}

/**
 * The height of the page, in page units, that gives the reference panel the shape [sizing] asks
 * for.
 *
 * The reference panel's width is fixed by the margin, the gutters, and the column weights alone,
 * so the height that makes it come out at the wanted ratio can be solved for directly. Because it
 * is solved every time rather than stored, the promise still holds after the margin, the gutter,
 * or the grid changes underneath it.
 */
fun pageHeightOf(
    sizing: PageSizing,
    layout: Layout,
    style: ComicStyle,
    panels: List<Panel>,
): Float {
    val ratio = when (sizing) {
        is PageSizing.Ratio -> sizing.value
        // An empty auto panel has nothing to take a shape from, so it waits as a square.
        PageSizing.FromImage -> panels.firstOrNull()?.image?.sourceAspect ?: SQUARE_RATIO
    }
    if (ratio <= 0f) return SQUARE_RATIO
    val grid = layout.grid
    val span = grid.panelSpans().firstOrNull() ?: return 1f / ratio
    val halfGutter = style.gutter / 2f

    val columnFraction = grid.columnWeights.fractionOf(span.firstColumn, span.lastColumn)
    val rowFraction = grid.rowWeights.fractionOf(span.firstRow, span.lastRow)
    if (rowFraction <= 0f) return SQUARE_RATIO

    val widthInset = halfGutter * interiorEdges(span.firstColumn, span.lastColumn, grid.columns)
    val heightInset = halfGutter * interiorEdges(span.firstRow, span.lastRow, grid.rows)

    val width = (1f - 2f * style.pageMargin) * columnFraction - widthInset
    if (width <= 0f) return SQUARE_RATIO
    val contentHeight = (width / ratio + heightInset) / rowFraction
    return (contentHeight + 2f * style.pageMargin).coerceIn(MIN_PAGE_HEIGHT, MAX_PAGE_HEIGHT)
}

/** The share of the whole extent taken by the weights from [first] to [last]. */
private fun List<Float>.fractionOf(first: Int, last: Int): Float {
    val total = sum()
    if (total <= 0f) return 0f
    return subList(first.coerceAtLeast(0), (last + 1).coerceAtMost(size)).sum() / total
}

/** How many of a span's edges face a neighbour, and so carry half a gutter. */
private fun interiorEdges(first: Int, last: Int, count: Int): Float =
    (if (first > 0) 1f else 0f) + (if (last < count - 1) 1f else 0f)
