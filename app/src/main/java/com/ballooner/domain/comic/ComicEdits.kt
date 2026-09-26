package com.ballooner.domain.comic

/** How the panels of a new layout inherit content from the panels of the old one. */
enum class PanelMatching {

    /**
     * Each new panel takes the content of the old panel it overlaps most, and where several new
     * panels want the same old one, the first in reading order wins. This is how every local edit
     * behaves: splitting a panel leaves the content with the first piece, and merging panels
     * leaves it with the first of them.
     */
    BY_OVERLAP,

    /**
     * Panels are paired off by position in reading order. This is how switching preset or
     * changing a grid's dimensions behaves, where the old and new panels need not overlap at all.
     */
    BY_INDEX,
}

/** The result of changing a layout, and what changing it would throw away. */
data class LayoutChange(
    val comic: Comic,
    val removedImages: Int,
    val removedBalloons: Int,
) {
    val isDestructive: Boolean get() = removedImages > 0 || removedBalloons > 0
}

/**
 * Re-lays out this comic, carrying images and balloons across to the new panels.
 *
 * Nothing is applied on the caller's behalf: the returned [LayoutChange] holds both the new comic
 * and the count of what it would discard, so a destructive change can be confirmed first.
 */
fun Comic.withLayout(layout: Layout, matching: PanelMatching = PanelMatching.BY_OVERLAP): LayoutChange {
    val newShapes = panelShapes(layout, pageShape, style)
    val sources = when (matching) {
        PanelMatching.BY_OVERLAP -> panelMapping(panelShapes(), newShapes)
        PanelMatching.BY_INDEX -> List(newShapes.size) { index -> index.takeIf { it < panels.size } }
    }

    val newPanels = sources.map { source -> source?.let { panels.getOrNull(it) } ?: Panel() }
    val newIndexOfOld = sources
        .withIndex()
        .mapNotNull { (newIndex, source) -> source?.let { it to newIndex } }
        .toMap()

    val keptBalloons = balloons.mapNotNull { balloon ->
        when (val scope = balloon.scope) {
            is BalloonScope.Panel -> newIndexOfOld[scope.panelIndex]
                ?.let { balloon.copy(scope = BalloonScope.Panel(it)) }
            BalloonScope.Comic -> balloon
        }
    }

    return LayoutChange(
        comic = copy(layout = layout, panels = newPanels, balloons = keptBalloons),
        removedImages = panels.indices.count { panels[it].image != null && it !in newIndexOfOld },
        removedBalloons = balloons.size - keptBalloons.size,
    )
}

/**
 * Which old panel each new panel inherits from, by index, or null where a new panel starts empty.
 *
 * Old panels are scanned in reading order and a candidate only wins by a clear margin, so when a
 * new panel overlaps two old ones equally — exactly what merging produces — the earlier one wins.
 */
fun panelMapping(old: List<Polygon>, new: List<Polygon>): List<Int?> {
    val claimed = mutableSetOf<Int>()
    return new.map { panel ->
        var bestIndex: Int? = null
        var bestArea = 0f
        old.forEachIndexed { index, candidate ->
            val area = intersectionArea(panel, candidate)
            if (area > bestArea + OVERLAP_TIE_TOLERANCE) {
                bestArea = area
                bestIndex = index
            }
        }
        if (bestArea <= MIN_OVERLAP_AREA) null else bestIndex?.takeIf { claimed.add(it) }
    }
}

private const val MIN_OVERLAP_AREA = 1e-6f
private const val OVERLAP_TIE_TOLERANCE = 1e-5f
