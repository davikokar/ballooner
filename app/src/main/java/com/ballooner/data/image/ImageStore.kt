package com.ballooner.data.image

import com.ballooner.domain.model.ImagePosition
import com.ballooner.domain.model.RectFraction

/** Stores comic images in app-private storage and cleans them up. */
interface ImageStore {
    /** Copies [sourceUri] into app storage and returns the local uri, or null on failure. */
    suspend fun importImage(sourceUri: String): String?

    /** Deletes the file behind [uri] if it is an app-owned image copy. */
    suspend fun deleteImage(uri: String)

    /**
     * Merges [addedUri] alongside [existingUri] into one new image, placed per [position]. The
     * added image is center-cropped to fill a box [widthSpan] by [heightSpan] times a standard
     * panel unit (the existing image's height for left/right, its width for top/bottom), so it
     * can occupy the footprint of multiple standard panels in either dimension; the existing
     * content is centered ("letterboxed") if the canvas grows to fit a larger added panel.
     * Returns the merged image's uri, the fractional rect the existing image now occupies within
     * it, and the fractional rect the added image now occupies (so callers can remap anything
     * positioned relative to the old image, or track the new panel), or null on failure.
     */
    suspend fun composeImages(
        existingUri: String,
        addedUri: String,
        position: ImagePosition,
        widthSpan: Int = 1,
        heightSpan: Int = 1,
    ): ComposedImage?
}

/** The result of [ImageStore.composeImages]. */
data class ComposedImage(val uri: String, val previousImageRect: RectFraction, val newImageRect: RectFraction)

