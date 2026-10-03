package com.ballooner.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri

/**
 * Which way up the image behind [sourceUri] was taken.
 *
 * Cameras usually record a photo in the sensor's own orientation and leave a note saying which
 * way up it was, so a portrait photo decodes as landscape unless that note is read.
 */
fun imageOrientation(context: Context, sourceUri: String): Int = runCatching {
    context.contentResolver.openInputStream(Uri.parse(sourceUri))?.use { stream ->
        ExifInterface(stream).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
    }
}.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

/** Whether [orientation] lays the image on its side, so its width and height trade places. */
fun turnsSideways(orientation: Int): Boolean = when (orientation) {
    ExifInterface.ORIENTATION_ROTATE_90,
    ExifInterface.ORIENTATION_ROTATE_270,
    ExifInterface.ORIENTATION_TRANSPOSE,
    ExifInterface.ORIENTATION_TRANSVERSE,
    -> true
    else -> false
}

/** Turns a decoded bitmap the way [orientation] says it was meant to be seen. */
fun Bitmap.uprighted(orientation: Int): Bitmap {
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
