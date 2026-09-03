package com.ballooner.data.image

import com.ballooner.domain.model.ImagePlacement
import com.ballooner.domain.model.RectFraction
import kotlinx.coroutines.CompletableDeferred

/** Records deletions and echoes imports so tests can assert cleanup. */
class FakeImageStore : ImageStore {
    val deleted = mutableListOf<String>()

    /** Set by tests to control what [composeImages] returns. */
    var composeResult: ComposedImage? = null
    var lastComposeRequest: List<Any>? = null

    /** Set by tests to control what [removeRegion] returns. */
    var removeResult: String? = null
    var lastRemoveRequest: Triple<String, RectFraction, RectFraction>? = null
    var rearrangeResult: RearrangedImage? = null
    var rearrangeGate: CompletableDeferred<Unit>? = null
    var rearrangeStarted = false
    var lastRearrangeRequest: List<Any>? = null
    var cropResult: String? = null
    var lastCropRequest: List<Any>? = null
    var initialGridResult: InitialImageGrid? = null
    var lastInitialGridRequest: Pair<List<String>, Int>? = null

    override suspend fun importImage(sourceUri: String): String = sourceUri

    override suspend fun createInitialGrid(sourceUris: List<String>, columns: Int): InitialImageGrid? {
        lastInitialGridRequest = sourceUris to columns
        return initialGridResult
    }

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

    override suspend fun rearrangePanels(
        uri: String,
        panels: List<RectFraction>,
        fromIndex: Int,
        destination: RectFraction,
    ): RearrangedImage? {
        lastRearrangeRequest = listOf(uri, panels, fromIndex, destination)
        rearrangeStarted = true
        rearrangeGate?.await()
        return rearrangeResult
    }

    override suspend fun cropPanel(
        uri: String,
        panel: RectFraction,
        frame: RectFraction,
        imageBounds: RectFraction,
    ): String? {
        lastCropRequest = listOf(uri, panel, frame, imageBounds)
        return cropResult
    }
}
