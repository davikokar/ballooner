package com.ballooner.data.image

import com.ballooner.domain.model.ImagePlacement
import com.ballooner.domain.model.RectFraction

/** Stores comic images in app-private storage and cleans them up. */
interface ImageStore {
    /** Copies [sourceUri] into app storage and returns the local uri, or null on failure. */
    suspend fun importImage(sourceUri: String): String?

    /** Creates the first comic image by arranging all selected images in a column-based grid. */
    suspend fun createInitialGrid(sourceUris: List<String>, columns: Int): InitialImageGrid?

    /** Deletes the file behind [uri] if it is an app-owned image copy. */
    suspend fun deleteImage(uri: String)

    /** Merges [addedUri] beside the selected anchor while preserving its aspect ratio. */
    suspend fun composeImages(
        existingUri: String,
        addedUri: String,
        placement: ImagePlacement,
    ): ComposedImage?

    /** Erases [removed] and crops the image to [retained], returning the new local uri. */
    suspend fun removeRegion(uri: String, removed: RectFraction, retained: RectFraction): String?

    /** Rebuilds the flattened image after freely moving one panel to [destination]. */
    suspend fun rearrangePanels(
        uri: String,
        panels: List<RectFraction>,
        fromIndex: Int,
        destination: RectFraction,
    ): RearrangedImage?

    /** Replaces [panel] with [source] scaled to fill the same panel rectangle. */
    suspend fun cropPanel(uri: String, panel: RectFraction, source: RectFraction): String?
}

/** The result of [ImageStore.composeImages]. */
data class ComposedImage(val uri: String, val previousImageRect: RectFraction, val newImageRect: RectFraction)

/** New panel rectangles correspond by index to the input panel list. */
data class RearrangedImage(val uri: String, val panelRects: List<RectFraction>)

data class InitialImageGrid(val uri: String, val panelRects: List<RectFraction>)

