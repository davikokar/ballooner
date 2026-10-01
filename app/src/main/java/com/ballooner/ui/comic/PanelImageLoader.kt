package com.ballooner.ui.comic

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.ballooner.data.image.imageOrientation
import com.ballooner.data.image.uprighted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Loads the bitmaps behind a comic's panel images, keeping what it has already decoded.
 *
 * Decoding is capped well above what any panel needs on screen but well below a full camera
 * image, because the document is redrawn from these on every frame.
 */
@Composable
fun rememberPanelImageSource(sourceUris: Set<String>): PanelImageSource {
    val context = LocalContext.current
    val loaded = remember { mutableStateMapOf<String, ImageBitmap>() }

    LaunchedEffect(sourceUris) {
        // A page filled in one trip to the picker would otherwise appear one image at a time.
        coroutineScope {
            sourceUris.filterNot { it in loaded }.map { uri ->
                async {
                    withContext(Dispatchers.IO) { decode(context, uri) }?.let { loaded[uri] = it }
                }
            }.awaitAll()
        }
    }

    return remember(loaded) { PanelImageSource { loaded[it] } }
}

private fun decode(context: Context, sourceUri: String, maxEdge: Int = MAX_EDGE_PIXELS): ImageBitmap? =
    runCatching {
        val uri = sourceUri.toUri()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        val longestEdge = maxOf(bounds.outWidth, bounds.outHeight)
        var sampleSize = 1
        while (longestEdge / sampleSize > maxEdge) sampleSize *= 2

        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        // Imported copies keep the file they came from, note and all, so which way up the photo
        // was taken is settled here rather than by rewriting it on the way in.
        val orientation = imageOrientation(context, sourceUri)
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)?.uprighted(orientation)?.asImageBitmap()
        }
    }.getOrNull()

/**
 * Decodes the images behind [sourceUris] as large as export allows.
 *
 * Editing works from smaller copies so the page can be redrawn every frame, but an export that
 * used those would be softer than the pictures it was made from.
 */
suspend fun loadImagesForExport(context: Context, sourceUris: Set<String>): PanelImageSource {
    val decoded = withContext(Dispatchers.IO) {
        sourceUris.mapNotNull { uri -> decode(context, uri, EXPORT_EDGE_PIXELS)?.let { uri to it } }.toMap()
    }
    return PanelImageSource { decoded[it] }
}

private const val MAX_EDGE_PIXELS = 2048
private const val EXPORT_EDGE_PIXELS = 4096
