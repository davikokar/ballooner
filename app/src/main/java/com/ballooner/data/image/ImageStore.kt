package com.ballooner.data.image

import com.ballooner.domain.model.ImagePosition

/** Stores comic images in app-private storage and cleans them up. */
interface ImageStore {
    /** Copies [sourceUri] into app storage and returns the local uri, or null on failure. */
    suspend fun importImage(sourceUri: String): String?

    /** Deletes the file behind [uri] if it is an app-owned image copy. */
    suspend fun deleteImage(uri: String)

    /**
     * Merges [addedUri] alongside [existingUri] into one new image, placed per [position] and
     * scaled to match the existing image's height (left/right) or width (top/bottom).
     * Returns the merged image's uri and the fractional rect the existing image now occupies
     * within it (so callers can remap anything positioned relative to the old image), or null
     * on failure.
     */
    suspend fun composeImages(existingUri: String, addedUri: String, position: ImagePosition): ComposedImage?
}

/** The result of [ImageStore.composeImages]. */
data class ComposedImage(val uri: String, val previousImageRect: RectFraction)

/** A rectangle expressed as fractions (0f..1f) of a larger canvas. */
data class RectFraction(val left: Float, val top: Float, val width: Float, val height: Float)

