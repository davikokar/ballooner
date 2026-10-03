package com.ballooner.ui.comic

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.core.content.FileProvider
import com.ballooner.domain.comic.Comic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** What a comic is, once it has left the app. */
internal const val PNG_TYPE = "image/png"

/** Where a comic waits while another app is deciding what to do with it. */
private const val SHARED_DIRECTORY = "shared"

/**
 * Renders [comic] and hands it to whatever the user wants to send it with.
 *
 * The PNG is written to the app's own cache and offered through the file provider rather than
 * written to storage: sharing is passing a copy on, not keeping one, which is what exporting does.
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
    val bitmap = renderComicBitmap(context, comic, density, fontFamilyResolver) ?: return false
    val file = withContext(Dispatchers.IO) {
        runCatching {
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

/**
 * Renders [comic] into [target], the document the user picked to keep it in.
 *
 * Returns false when the comic could not be rendered or written.
 */
suspend fun exportComicPng(
    context: Context,
    comic: Comic,
    target: Uri,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver,
): Boolean {
    val bitmap = renderComicBitmap(context, comic, density, fontFamilyResolver) ?: return false
    return withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(target)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            } ?: false
        }.getOrDefault(false)
    }
}

/** The comic's page at full size, which is what both sharing and exporting hand on. */
private suspend fun renderComicBitmap(
    context: Context,
    comic: Comic,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver,
): Bitmap? {
    // Export decodes its own, larger copies: the editing ones are deliberately small.
    val images = loadImagesForExport(context, comic.panels.mapNotNull { it.image?.sourceUri }.toSet())
    return withContext(Dispatchers.Default) {
        runCatching {
            renderComic(
                comic = comic,
                images = images,
                pixelWidth = comicPixelWidth(comic) { uri ->
                    images.bitmapFor(uri)?.let { IntSize(it.width, it.height) }
                },
                density = density,
                fontFamilyResolver = fontFamilyResolver,
            ).asAndroidBitmap()
        }.getOrNull()
    }
}

/** A comic may be called anything; a file may not. */
private fun String.fileSafe(): String =
    filter { it.isLetterOrDigit() || it == ' ' || it == '-' || it == '_' }
        .trim()
        .ifBlank { "comic" }
        .take(60)
