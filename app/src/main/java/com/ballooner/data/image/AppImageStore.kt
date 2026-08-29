package com.ballooner.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import com.ballooner.domain.model.ImagePosition
import com.ballooner.domain.model.RectFraction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlin.math.roundToInt

class AppImageStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : ImageStore {

    private val imagesDir = File(context.filesDir, "images")

    override suspend fun importImage(sourceUri: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            // Decoded mutable so the border can be drawn directly onto it, instead of
            // allocating a second full-size bitmap just to hold a copy plus a border.
            val bitmap = decodeBitmap(sourceUri, mutable = true) ?: return@runCatching null
            val canvas = android.graphics.Canvas(bitmap)
            val borderPx = borderThicknessPx(bitmap.width, bitmap.height)
            canvas.drawRect(borderRect(0, 0, bitmap.width, bitmap.height, borderPx / 2f), borderPaint(borderPx))
            imagesDir.mkdirs()
            val dest = File(imagesDir, "img_${System.currentTimeMillis()}.png")
            dest.outputStream().use { output -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, output) }
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
        widthSpan: Int,
        heightSpan: Int,
    ): ComposedImage? = withContext(Dispatchers.IO) {
        runCatching {
            val existingBitmap = decodeBitmap(existingUri) ?: return@runCatching null
            val addedBitmap = decodeBitmap(addedUri) ?: return@runCatching null
            val layout = computeComposeLayout(
                existingWidth = existingBitmap.width,
                existingHeight = existingBitmap.height,
                position = position,
                widthSpan = widthSpan,
                heightSpan = heightSpan,
            )
            val scaledAdded = centerCrop(addedBitmap, layout.scaledAddedWidth, layout.scaledAddedHeight)
            val composite = Bitmap.createBitmap(layout.canvasWidth, layout.canvasHeight, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(composite)
            // Paper-white background shows through the gap between the two panels (and any
            // letterboxing when the added panel's span makes it larger than the existing one).
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
            ComposedImage(
                uri = Uri.fromFile(dest).toString(),
                previousImageRect = layout.existingRect,
                newImageRect = layout.addedRect,
            )
        }.getOrNull()
    }

    override suspend fun eraseRegion(uri: String, rect: RectFraction): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = decodeBitmap(uri, mutable = true) ?: return@runCatching null
            val canvas = android.graphics.Canvas(bitmap)
            val left = rect.left * bitmap.width
            val top = rect.top * bitmap.height
            val fillPaint = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }
            canvas.drawRect(left, top, left + rect.width * bitmap.width, top + rect.height * bitmap.height, fillPaint)
            imagesDir.mkdirs()
            val dest = File(imagesDir, "img_${System.currentTimeMillis()}.png")
            dest.outputStream().use { output -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, output) }
            Uri.fromFile(dest).toString()
        }.getOrNull()
    }

    private fun borderPaint(strokeWidthPx: Int) = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx.toFloat()
    }

    /** Scales [bitmap] to cover [targetWidth]x[targetHeight], then crops the centered excess. */
    private fun centerCrop(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val scale = maxOf(targetWidth.toFloat() / bitmap.width, targetHeight.toFloat() / bitmap.height)
        val scaledWidth = (bitmap.width * scale).roundToInt().coerceAtLeast(targetWidth)
        val scaledHeight = (bitmap.height * scale).roundToInt().coerceAtLeast(targetHeight)
        val scaled = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
        return Bitmap.createBitmap(
            scaled,
            (scaledWidth - targetWidth) / 2,
            (scaledHeight - targetHeight) / 2,
            targetWidth,
            targetHeight,
        )
    }

    private fun borderRect(left: Int, top: Int, width: Int, height: Int, inset: Float) = android.graphics.RectF(
        left + inset,
        top + inset,
        left + width - inset,
        top + height - inset,
    )

    /**
     * Decodes [uri] downsampled so neither dimension exceeds [MAX_DECODED_DIMENSION_PX] — full
     * camera-resolution photos (often 12+ MP) would otherwise risk an OutOfMemoryError once
     * decoded, copied, and re-encoded.
     *
     * Reads the source into a byte array first: some content providers (e.g. the photo picker)
     * hand out a one-shot stream that can't be reopened for a second (bounds-then-decode) pass.
     */
    private fun decodeBitmap(uri: String, mutable: Boolean = false): Bitmap? {
        val bytes = context.contentResolver.openInputStream(Uri.parse(uri))?.use { it.readBytes() } ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= MAX_DECODED_DIMENSION_PX) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inMutable = mutable
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private companion object {
        const val MAX_DECODED_DIMENSION_PX = 2048
    }
}

