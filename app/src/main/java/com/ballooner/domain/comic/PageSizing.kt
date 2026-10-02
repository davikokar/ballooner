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
 * Loose enough to stay out of the way of a real layout: a strip has no limit on how many panels
 * it holds, and the page has to keep the shape it promised each of them. These only catch a page
 * that has genuinely degenerated.
 */
const val MIN_PAGE_HEIGHT = 0.01f
const val MAX_PAGE_HEIGHT = 100f

/**
 * What gives the page its height.
 *
 * The page is always one unit wide, so its height is the only measurement the document has to
 * supply. Nothing else can supply it: grid weights divide whatever height they are given, and cuts
 * are stored in normalized coordinates, so both describe proportions *between* panels and never an
 * absolute size. The page therefore takes its height from the shape of one cell — the reference
 * cell, which is the grid's top-left — and the page shape stops being a thing the user picks at
 * all.
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
    val gutter = style.gutter

    // The reference is the top-left CELL, never whatever panel happens to be first. A merged
    // panel is made of cells, so holding a merge to the chosen shape would squash every cell it
    // did not cover; holding one cell to it keeps them all that shape and lets a merge be a
    // multiple of it. The top row may divide its own width, so the cell is read through it.
    val columnFraction = grid.columnWeightsAt(0).firstFraction()
    val rowFraction = grid.rowWeights.firstFraction()
    if (rowFraction <= 0f) return SQUARE_RATIO

    // The gutters are fixed, so they come out of the extent before the weights divide the rest.
    val available = ((1f - 2f * style.pageMargin) - gutter * (grid.columns - 1)).coerceAtLeast(0f)
    val width = available * columnFraction
    if (width <= 0f) return SQUARE_RATIO

    val contentHeight = width / ratio / rowFraction + gutter * (grid.rows - 1)
    return (contentHeight + 2f * style.pageMargin).coerceIn(MIN_PAGE_HEIGHT, MAX_PAGE_HEIGHT)
}

/**
 * The shape the reference cell comes out at on a page of [pageHeight] — the inverse of
 * [pageHeightOf].
 *
 * Moving the grid line beside the reference cell reshapes that cell, which would otherwise
 * reshape the whole page and every other panel with it. Reading the ratio back out lets the page
 * keep the height it had, so only the two panels either side of the line change.
 */
fun referenceRatioOf(layout: Layout, style: ComicStyle, pageHeight: Float): Float {
    val grid = layout.grid
    val gutter = style.gutter
    val available = ((1f - 2f * style.pageMargin) - gutter * (grid.columns - 1)).coerceAtLeast(0f)
    val width = available * grid.columnWeightsAt(0).firstFraction()
    val contentHeight = pageHeight - 2f * style.pageMargin
    val height = (contentHeight - gutter * (grid.rows - 1)) * grid.rowWeights.firstFraction()
    if (width <= 0f || height <= 0f) return SQUARE_RATIO
    return width / height
}

/** The share of the whole extent taken by the first row or column. */
private fun List<Float>.firstFraction(): Float {
    val total = sum()
    if (total <= 0f || isEmpty()) return 0f
    return first() / total
}
