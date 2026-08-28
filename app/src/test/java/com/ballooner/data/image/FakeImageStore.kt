package com.ballooner.data.image

import com.ballooner.domain.model.ImagePosition

/** Records deletions and echoes imports so tests can assert cleanup. */
class FakeImageStore : ImageStore {
    val deleted = mutableListOf<String>()

    /** Set by tests to control what [composeImages] returns. */
    var composeResult: ComposedImage? = null
    var lastComposeRequest: List<Any>? = null

    override suspend fun importImage(sourceUri: String): String = sourceUri

    override suspend fun deleteImage(uri: String) {
        deleted += uri
    }

    override suspend fun composeImages(
        existingUri: String,
        addedUri: String,
        position: ImagePosition,
        sizeSpan: Int,
    ): ComposedImage? {
        lastComposeRequest = listOf(existingUri, addedUri, position, sizeSpan)
        return composeResult
    }
}
