package com.ballooner.data.comic

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.ballooner.data.image.ImageStore
import com.ballooner.data.image.imageOrientation
import com.ballooner.data.image.turnsSideways
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
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
        if (options.outWidth <= 0 || options.outHeight <= 0) return null
        // The header describes the pixels as stored, which for a photo taken on its side is the
        // other way round from how it will be shown.
        val sideways = turnsSideways(imageOrientation(context, uri))
        val width = if (sideways) options.outHeight else options.outWidth
        val height = if (sideways) options.outWidth else options.outHeight
        width.toFloat() / height
    }.getOrNull()
}
