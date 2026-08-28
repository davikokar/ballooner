package com.ballooner.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.ballooner.domain.model.ImagePosition
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class AppImageStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : ImageStore {

    private val imagesDir = File(context.filesDir, "images")

    override suspend fun importImage(sourceUri: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            imagesDir.mkdirs()
            val dest = File(imagesDir, "img_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(Uri.parse(sourceUri))?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return@runCatching null
            Uri.fromFile(dest).toString()
        }.getOrNull()
    }

    override suspend fun deleteImage(uri: String) {
        withContext(Dispatchers.IO) {
            runCatching {
                val file = Uri.parse(uri).path?.let(::File) ?: return@runCatching
                // Only delete copies we own, never the user's original.
                if (file.parentFile?.absolutePath == imagesDir.absolutePath && file.exists()) {
                    file.delete()
                }
            }
        }
    }

    override suspend fun composeImages(
        existingUri: String,
        addedUri: String,
        position: ImagePosition,
    ): ComposedImage? = withContext(Dispatchers.IO) {
        runCatching {
            val existingBitmap = decodeBitmap(existingUri) ?: return@runCatching null
            val addedBitmap = decodeBitmap(addedUri) ?: return@runCatching null
            val layout = computeComposeLayout(
                existingWidth = existingBitmap.width,
                existingHeight = existingBitmap.height,
                addedWidth = addedBitmap.width,
                addedHeight = addedBitmap.height,
                position = position,
            )
            val scaledAdded = Bitmap.createScaledBitmap(
                addedBitmap,
                layout.scaledAddedWidth,
                layout.scaledAddedHeight,
                true,
            )
            val composite = Bitmap.createBitmap(layout.canvasWidth, layout.canvasHeight, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(composite)
            val existingLeft = (layout.existingRect.left * layout.canvasWidth).toInt()
            val existingTop = (layout.existingRect.top * layout.canvasHeight).toInt()
            val addedLeft = if (position == ImagePosition.LEFT) 0 else if (position == ImagePosition.RIGHT) existingBitmap.width else 0
            val addedTop = if (position == ImagePosition.TOP) 0 else if (position == ImagePosition.BOTTOM) existingBitmap.height else 0
            canvas.drawBitmap(existingBitmap, existingLeft.toFloat(), existingTop.toFloat(), null)
            canvas.drawBitmap(scaledAdded, addedLeft.toFloat(), addedTop.toFloat(), null)
            imagesDir.mkdirs()
            val dest = File(imagesDir, "img_${System.currentTimeMillis()}.png")
            dest.outputStream().use { output -> composite.compress(Bitmap.CompressFormat.PNG, 100, output) }
            ComposedImage(uri = Uri.fromFile(dest).toString(), previousImageRect = layout.existingRect)
        }.getOrNull()
    }

    private fun decodeBitmap(uri: String): Bitmap? =
        context.contentResolver.openInputStream(Uri.parse(uri))?.use { stream -> BitmapFactory.decodeStream(stream) }
}

