package com.ballooner.data.image

import com.ballooner.domain.model.ImagePlacement
import com.ballooner.domain.model.RectFraction

/** Stores comic images in app-private storage and cleans them up. */
interface ImageStore {
    /** Copies [sourceUri] into app storage and returns the local uri, or null on failure. */
    suspend fun importImage(sourceUri: String): String?

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

    /** Rebuilds the flattened image after moving one panel in reading order. */
    suspend fun rearrangePanels(
        uri: String,
        panels: List<RectFraction>,
        fromIndex: Int,
        toIndex: Int,
    ): RearrangedImage?
}

/** The result of [ImageStore.composeImages]. */
data class ComposedImage(val uri: String, val previousImageRect: RectFraction, val newImageRect: RectFraction)

/** New panel rectangles correspond by index to the input panel list. */
data class RearrangedImage(val uri: String, val panelRects: List<RectFraction>)

