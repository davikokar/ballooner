package com.ballooner.domain.comic

import com.ballooner.domain.model.BalloonFont
import com.ballooner.domain.model.BalloonType

/**
 * How an image sits inside its panel. The image file itself is never modified; this is the whole
 * of what the Placement step records.
 */
data class PanelImage(
    val sourceUri: String,
    /** The point of the image shown at the panel's centre, in image-local fractions. */
    val centre: NormalizedPoint = NormalizedPoint(0.5f, 0.5f),
    /**
     * A multiple of [coverScale], so 1 exactly fills the panel and anything above 1 fills it with
     * room to spare. Because it is relative, the same value keeps covering after the panel is
     * reshaped or the image is turned.
     */
    val zoom: Float = 1f,
    val angleDegrees: Float = 0f,
    /**
     * The image file's own width divided by its height, recorded when it was imported so the page
     * can take its shape from the image without decoding it. Null for an image imported before
     * this was kept.
     */
    val sourceAspect: Float? = null,
)

/** One panel's content. The panel's shape is derived from the layout and is not stored here. */
data class Panel(val image: PanelImage? = null)

/** Which part of the comic a balloon belongs to. */
sealed interface BalloonScope {

    /** A panel balloon, clipped to the panel at [panelIndex] in reading order. */
    data class Panel(val panelIndex: Int) : BalloonScope

    /** A comic balloon, drawn above every panel and clipped only by the page. */
    data object Comic : BalloonScope
}

/**
 * A balloon. Every size is a fraction of the page width — one uniform unit for both axes — so a
 * balloon never stretches when its panel changes shape.
 */
data class Balloon(
    val id: Long = 0,
    val type: BalloonType,
    val scope: BalloonScope,
    val text: String = "",
    /** Panel-local for a panel balloon, page-local for a comic balloon. */
    val centre: NormalizedPoint = NormalizedPoint(0.5f, 0.5f),
    val width: Float = 0.4f,
    val height: Float = 0.25f,
    val tailAngleDegrees: Float = 90f,
    val tailLength: Float = 0.12f,
    val tailWidth: Float = 0.5f,
    val cornerRoundness: Float = 1f,
    val fontSize: Float = 0.05f,
    /** When set, the text is sized to fill the balloon rather than held at [fontSize]. */
    val autoSize: Boolean = false,
    val font: BalloonFont = BalloonFont.ANIME_ACE,
)

/**
 * One comic: a single page, its layout, what fills each panel, and the balloons over it.
 *
 * [panels] is index-aligned with the shapes the layout produces, in reading order. Balloons are
 * drawn in list order, which is the order they were added.
 */
data class Comic(
    val name: String = "",
    val sizing: PageSizing = PageSizing.Ratio(TALL_RATIO),
    val style: ComicStyle = ComicStyle(),
    val layout: Layout = Layout(Grid(rows = 1, columns = 1)),
    val panels: List<Panel> = listOf(Panel()),
    val balloons: List<Balloon> = emptyList(),
) {
    /** Derived, never stored: the page is shaped by its panels rather than the other way round. */
    val pageHeight: Float get() = pageHeightOf(sizing, layout, style, panels)
}

fun Comic.panelShapes(): List<Polygon> = panelShapes(layout, pageHeight, style)
