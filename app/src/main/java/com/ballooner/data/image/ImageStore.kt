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
     * added image is scaled so its matched dimension (height for left/right, width for
     * top/bottom) is [sizeSpan] times the existing image's, so it can occupy the same footprint
     * as one or two standard panels; the existing content is centered ("letterboxed") if the
     * canvas grows to fit a larger added panel.
     * Returns the merged image's uri, the fractional rect the existing image now occupies within
     * it, and the fractional rect the added image now occupies (so callers can remap anything
     * positioned relative to the old image, or track the new panel), or null on failure.
     */
    suspend fun composeImages(
        existingUri: String,
        addedUri: String,
        position: ImagePosition,
        sizeSpan: Int = 1,
    ): ComposedImage?
}

/** The result of [ImageStore.composeImages]. */
data class ComposedImage(val uri: String, val previousImageRect: RectFraction, val newImageRect: RectFraction)

