package com.ballooner.data.image

import com.ballooner.domain.model.ImagePosition
import com.ballooner.domain.model.RectFraction

/** Records deletions and echoes imports so tests can assert cleanup. */
class FakeImageStore : ImageStore {
    val deleted = mutableListOf<String>()

    /** Set by tests to control what [composeImages] returns. */
    var composeResult: ComposedImage? = null
    var lastComposeRequest: List<Any>? = null

    /** Set by tests to control what [eraseRegion] returns. */
    var eraseResult: String? = null
    var lastEraseRequest: Pair<String, RectFraction>? = null

    override suspend fun importImage(sourceUri: String): String = sourceUri

    override suspend fun deleteImage(uri: String) {
        deleted += uri
    }

    override suspend fun composeImages(
        existingUri: String,
        addedUri: String,
        position: ImagePosition,
        widthSpan: Int,
        heightSpan: Int,
    ): ComposedImage? {
        lastComposeRequest = listOf(existingUri, addedUri, position, widthSpan, heightSpan)
        return composeResult
    }

    override suspend fun eraseRegion(uri: String, rect: RectFraction): String? {
        lastEraseRequest = uri to rect
        return eraseResult
    }
}
