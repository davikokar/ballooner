package com.ballooner.data.image

import com.ballooner.domain.model.ImagePlacement
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
        placement: ImagePlacement,
        widthSpan: Int = 1,
        heightSpan: Int = 1,
    ): ComposedImage?

    /**
     * Paints over [rect] (a fraction of [uri]'s image) in solid paper white, e.g. to remove one
     * panel from a multi-image comic. Returns the new file's uri, or null on failure; the
     * original file at [uri] is left untouched, so callers should delete it once the new uri is
     * safely persisted.
     */
    suspend fun eraseRegion(uri: String, rect: RectFraction): String?
}

/** The result of [ImageStore.composeImages]. */
data class ComposedImage(val uri: String, val previousImageRect: RectFraction, val newImageRect: RectFraction)

