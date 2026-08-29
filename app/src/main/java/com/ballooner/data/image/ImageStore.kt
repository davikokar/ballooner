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

