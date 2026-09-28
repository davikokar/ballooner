package com.ballooner.data.comic

import android.graphics.BitmapFactory
import android.net.Uri
import com.ballooner.data.image.ImageStore
import javax.inject.Inject

/** A copied image, and the proportions the page can take its shape from. */
data class ImportedImage(val uri: String, val aspect: Float?)

/**
 * Copies a picked image into the app's own storage.
 *
 * A picked uri is only borrowed from whatever app provided it, so a comic that referred to it
 * directly would break as soon as that permission lapsed.
 */
fun interface PanelImageImporter {
    suspend fun import(sourceUri: String): ImportedImage?
}

class AppPanelImageImporter @Inject constructor(
    private val imageStore: ImageStore,
) : PanelImageImporter {

    override suspend fun import(sourceUri: String): ImportedImage? {
        val imported = imageStore.importImage(sourceUri) ?: return null
        return ImportedImage(imported, aspectOf(imported))
    }

    /** Reads the copy's proportions from its header, without decoding the pixels. */
    private fun aspectOf(uri: String): Float? = runCatching {
        val path = Uri.parse(uri).path ?: return null
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            null
        } else {
            options.outWidth.toFloat() / options.outHeight
        }
    }.getOrNull()
}
