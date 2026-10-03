package com.ballooner.ui.comic

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.core.content.FileProvider
import com.ballooner.domain.comic.Comic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val PNG_TYPE = "image/png"

/** Where a comic waits while another app is deciding what to do with it. */
private const val SHARED_DIRECTORY = "shared"

/**
 * Renders [comic] and hands it to whatever the user wants to send it with.
 *
 * The PNG is written to the app's own cache and offered through the file provider rather than
 * written to storage: sharing is passing a copy on, not saving one, and the Save in the editor is
 * what puts a comic somewhere it will keep.
 *
 * Returns false when the comic could not be rendered or written.
 */
suspend fun shareComicPng(
    context: Context,
    comic: Comic,
    name: String,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver,
): Boolean {
    val images = loadImagesForExport(context, comic.panels.mapNotNull { it.image?.sourceUri }.toSet())
    val file = withContext(Dispatchers.Default) {
        runCatching {
            val bitmap = renderComic(
                comic = comic,
                images = images,
                pixelWidth = comicPixelWidth(comic) { uri ->
                    images.bitmapFor(uri)?.let { IntSize(it.width, it.height) }
                },
                density = density,
                fontFamilyResolver = fontFamilyResolver,
            ).asAndroidBitmap()
            val directory = File(context.cacheDir, SHARED_DIRECTORY).apply { mkdirs() }
            // One file per comic, overwritten each time, so sharing twice does not litter.
            File(directory, "${name.fileSafe()}.png").also { target ->
                target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }.getOrNull()
    } ?: return false

    val uri = runCatching {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull() ?: return false

    val send = Intent(Intent.ACTION_SEND).apply {
        type = PNG_TYPE
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TITLE, name)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return runCatching { context.startActivity(chooser) }.isSuccess
}

/** A comic may be called anything; a file may not. */
private fun String.fileSafe(): String =
    filter { it.isLetterOrDigit() || it == ' ' || it == '-' || it == '_' }
        .trim()
        .ifBlank { "comic" }
        .take(60)
