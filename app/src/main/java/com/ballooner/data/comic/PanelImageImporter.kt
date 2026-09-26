package com.ballooner.data.comic

import com.ballooner.data.image.ImageStore
import javax.inject.Inject

/**
 * Copies a picked image into the app's own storage.
 *
 * A picked uri is only borrowed from whatever app provided it, so a comic that referred to it
 * directly would break as soon as that permission lapsed.
 */
fun interface PanelImageImporter {
    suspend fun import(sourceUri: String): String?
}

class AppPanelImageImporter @Inject constructor(
    private val imageStore: ImageStore,
) : PanelImageImporter {
    override suspend fun import(sourceUri: String): String? = imageStore.importImage(sourceUri)
}
