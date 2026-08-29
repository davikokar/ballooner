package com.ballooner.data.image

import com.ballooner.domain.model.ImagePlacement
import com.ballooner.domain.model.RectFraction

/** Records deletions and echoes imports so tests can assert cleanup. */
class FakeImageStore : ImageStore {
    val deleted = mutableListOf<String>()

    /** Set by tests to control what [composeImages] returns. */
    var composeResult: ComposedImage? = null
    var lastComposeRequest: List<Any>? = null

    /** Set by tests to control what [removeRegion] returns. */
    var removeResult: String? = null
    var lastRemoveRequest: Triple<String, RectFraction, RectFraction>? = null

    override suspend fun importImage(sourceUri: String): String = sourceUri

    override suspend fun deleteImage(uri: String) {
        deleted += uri
    }

    override suspend fun composeImages(
        existingUri: String,
        addedUri: String,
        placement: ImagePlacement,
    ): ComposedImage? {
        lastComposeRequest = listOf(existingUri, addedUri, placement)
        return composeResult
    }

    override suspend fun removeRegion(uri: String, removed: RectFraction, retained: RectFraction): String? {
        lastRemoveRequest = Triple(uri, removed, retained)
        return removeResult
    }
}
