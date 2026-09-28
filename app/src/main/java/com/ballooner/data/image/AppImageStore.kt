package com.ballooner.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
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
            val decoded = decodeBitmap(sourceUri) ?: return@runCatching null
            val bitmap = decoded.uprighted(orientationOf(sourceUri))
            imagesDir.mkdirs()
            val destination = File(imagesDir, "img_${System.currentTimeMillis()}.png")
            destination.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            Uri.fromFile(destination).toString()
        }.getOrNull()
    }

    /**
     * Cameras usually record a photo in the sensor's own orientation and leave a note saying
     * which way up it was, so a portrait photo decodes as landscape unless that note is read.
     */
    private fun orientationOf(sourceUri: String): Int = runCatching {
        context.contentResolver.openInputStream(Uri.parse(sourceUri))?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        }
    }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

    private fun Bitmap.uprighted(orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return this
        }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
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

    private fun decodeBitmap(uri: String): Bitmap? {
        val bytes = context.contentResolver.openInputStream(Uri.parse(uri))?.use { it.readBytes() }
            ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= MAX_IMPORTED_DIMENSION_PX) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private companion object {
        const val MAX_IMPORTED_DIMENSION_PX = 4096
    }
}
