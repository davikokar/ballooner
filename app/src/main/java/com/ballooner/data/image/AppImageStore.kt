package com.ballooner.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
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
            val bitmap = decodeBitmap(sourceUri) ?: return@runCatching null
            val bordered = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bordered)
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            val borderPx = borderThicknessPx(bitmap.width, bitmap.height)
            canvas.drawRect(borderRect(0, 0, bitmap.width, bitmap.height, borderPx / 2f), borderPaint(borderPx))
            imagesDir.mkdirs()
            val dest = File(imagesDir, "img_${System.currentTimeMillis()}.png")
            dest.outputStream().use { output -> bordered.compress(Bitmap.CompressFormat.PNG, 100, output) }
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
            // Paper-white background shows through the gap between the two panels.
            canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(existingBitmap, layout.existingLeft.toFloat(), layout.existingTop.toFloat(), null)
            canvas.drawBitmap(scaledAdded, layout.addedLeft.toFloat(), layout.addedTop.toFloat(), null)
            val existingInset = layout.existingBorderPx / 2f
            canvas.drawRect(
                borderRect(layout.existingLeft, layout.existingTop, existingBitmap.width, existingBitmap.height, existingInset),
                borderPaint(layout.existingBorderPx),
            )
            val addedInset = layout.addedBorderPx / 2f
            canvas.drawRect(
                borderRect(layout.addedLeft, layout.addedTop, layout.scaledAddedWidth, layout.scaledAddedHeight, addedInset),
                borderPaint(layout.addedBorderPx),
            )
            imagesDir.mkdirs()
            val dest = File(imagesDir, "img_${System.currentTimeMillis()}.png")
            dest.outputStream().use { output -> composite.compress(Bitmap.CompressFormat.PNG, 100, output) }
            ComposedImage(uri = Uri.fromFile(dest).toString(), previousImageRect = layout.existingRect)
        }.getOrNull()
    }

    private fun borderPaint(strokeWidthPx: Int) = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx.toFloat()
    }

    private fun borderRect(left: Int, top: Int, width: Int, height: Int, inset: Float) = android.graphics.RectF(
        left + inset,
        top + inset,
        left + width - inset,
        top + height - inset,
    )

    private fun decodeBitmap(uri: String): Bitmap? =
        context.contentResolver.openInputStream(Uri.parse(uri))?.use { stream -> BitmapFactory.decodeStream(stream) }
}

