package com.ballooner.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.net.Uri
import com.ballooner.domain.model.ImagePlacement
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

    override suspend fun createInitialGrid(sourceUris: List<String>, columns: Int): InitialImageGrid? =
        withContext(Dispatchers.IO) {
            runCatching {
                val bitmaps = sourceUris.map { uri -> decodeBitmap(uri) ?: return@runCatching null }
                if (bitmaps.isEmpty()) return@runCatching null
                val actualColumns = columns.coerceIn(1, bitmaps.size)
                val gapPx = MIN_PANEL_GAP_PX
                val columnWidth = ((MAX_GRID_WIDTH_PX - (actualColumns - 1) * gapPx) / actualColumns)
                    .coerceAtLeast(1)
                val layout = computeInitialGridLayout(
                    imageSizes = bitmaps.map { PixelSize(it.width, it.height) },
                    columns = actualColumns,
                    columnWidth = columnWidth,
                    gapPx = gapPx,
                )
                val composite = Bitmap.createBitmap(layout.canvasWidth, layout.canvasHeight, Bitmap.Config.ARGB_8888)
                composite.eraseColor(COMIC_CANVAS_BACKGROUND_COLOR)
                val canvas = android.graphics.Canvas(composite)
                bitmaps.forEachIndexed { index, bitmap ->
                    val target = layout.imageRects[index]
                    val scaled = Bitmap.createScaledBitmap(bitmap, target.width, target.height, true)
                    canvas.drawBitmap(scaled, target.left.toFloat(), target.top.toFloat(), null)
                    val borderWidth = borderThicknessPx(target.width, target.height)
                    canvas.drawRect(
                        borderRect(target.left, target.top, target.width, target.height, borderWidth / 2f),
                        borderPaint(borderWidth),
                    )
                }
                imagesDir.mkdirs()
                val dest = File(imagesDir, "img_${System.currentTimeMillis()}.png")
                dest.outputStream().use { output -> composite.compress(Bitmap.CompressFormat.PNG, 100, output) }
                InitialImageGrid(Uri.fromFile(dest).toString(), layout.panelRects)
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
        placement: ImagePlacement,
    ): ComposedImage? = withContext(Dispatchers.IO) {
        runCatching {
            val existingBitmap = decodeBitmap(existingUri) ?: return@runCatching null
            val addedBitmap = decodeBitmap(addedUri) ?: return@runCatching null
            val layout = computeComposeLayout(
                existingWidth = existingBitmap.width,
                existingHeight = existingBitmap.height,
                addedWidth = addedBitmap.width,
                addedHeight = addedBitmap.height,
                position = placement.position,
                anchor = placement.anchor,
            )
            val scaledAdded = Bitmap.createScaledBitmap(
                addedBitmap,
                layout.scaledAddedWidth,
                layout.scaledAddedHeight,
                true,
            )
            val composite = Bitmap.createBitmap(layout.canvasWidth, layout.canvasHeight, Bitmap.Config.ARGB_8888)
            composite.eraseColor(COMIC_CANVAS_BACKGROUND_COLOR)
            val canvas = android.graphics.Canvas(composite)
            canvas.drawBitmap(existingBitmap, layout.existingLeft.toFloat(), layout.existingTop.toFloat(), null)
            canvas.drawBitmap(scaledAdded, layout.addedLeft.toFloat(), layout.addedTop.toFloat(), null)
            layout.bordersToDraw.forEach { border ->
                canvas.drawRect(
                    borderRect(
                        border.left,
                        border.top,
                        border.width,
                        border.height,
                        border.strokeWidth / 2f,
                    ),
                    borderPaint(border.strokeWidth),
                )
            }
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

    override suspend fun removeRegion(
        uri: String,
        removed: RectFraction,
        retained: RectFraction,
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = decodeBitmap(uri, mutable = true) ?: return@runCatching null
            val canvas = android.graphics.Canvas(bitmap)
            val left = removed.left * bitmap.width
            val top = removed.top * bitmap.height
            val clearPaint = Paint().apply {
                style = Paint.Style.FILL
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            }
            canvas.drawRect(
                left,
                top,
                left + removed.width * bitmap.width,
                top + removed.height * bitmap.height,
                clearPaint,
            )
            val cropLeft = (retained.left * bitmap.width).roundToInt().coerceIn(0, bitmap.width - 1)
            val cropTop = (retained.top * bitmap.height).roundToInt().coerceIn(0, bitmap.height - 1)
            val cropRight = ((retained.left + retained.width) * bitmap.width)
                .roundToInt().coerceIn(cropLeft + 1, bitmap.width)
            val cropBottom = ((retained.top + retained.height) * bitmap.height)
                .roundToInt().coerceIn(cropTop + 1, bitmap.height)
            val cropped = Bitmap.createBitmap(bitmap, cropLeft, cropTop, cropRight - cropLeft, cropBottom - cropTop)
            imagesDir.mkdirs()
            val dest = File(imagesDir, "img_${System.currentTimeMillis()}.png")
            dest.outputStream().use { output -> cropped.compress(Bitmap.CompressFormat.PNG, 100, output) }
            Uri.fromFile(dest).toString()
        }.getOrNull()
    }

    override suspend fun rearrangePanels(
        uri: String,
        panels: List<RectFraction>,
        fromIndex: Int,
        destination: RectFraction,
    ): RearrangedImage? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = decodeBitmap(uri) ?: return@runCatching null
            val sourceRects = panels.map { it.toPixelRect(bitmap.width, bitmap.height) }
            val panelBitmaps = sourceRects.map { rect ->
                Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width, rect.height)
            }
            val layout = computeRearrangeLayout(
                panelRects = sourceRects,
                fromIndex = fromIndex,
                desiredLeft = (destination.left * bitmap.width).roundToInt(),
                desiredTop = (destination.top * bitmap.height).roundToInt(),
            )
            val composite = Bitmap.createBitmap(layout.canvasWidth, layout.canvasHeight, Bitmap.Config.ARGB_8888)
            composite.eraseColor(COMIC_CANVAS_BACKGROUND_COLOR)
            val canvas = android.graphics.Canvas(composite)
            panelBitmaps.forEachIndexed { index, panelBitmap ->
                val target = layout.panelRects[index]
                canvas.drawBitmap(panelBitmap, target.left.toFloat(), target.top.toFloat(), null)
            }
            imagesDir.mkdirs()
            val dest = File(imagesDir, "img_${System.currentTimeMillis()}.png")
            dest.outputStream().use { output -> composite.compress(Bitmap.CompressFormat.PNG, 100, output) }
            RearrangedImage(
                uri = Uri.fromFile(dest).toString(),
                panelRects = layout.panelRects.map { rect ->
                    RectFraction(
                        left = rect.left.toFloat() / layout.canvasWidth,
                        top = rect.top.toFloat() / layout.canvasHeight,
                        width = rect.width.toFloat() / layout.canvasWidth,
                        height = rect.height.toFloat() / layout.canvasHeight,
                    )
                },
            )
        }.getOrNull()
    }

    private fun RectFraction.toPixelRect(canvasWidth: Int, canvasHeight: Int): PixelRect {
        val pixelLeft = (left * canvasWidth).roundToInt().coerceIn(0, canvasWidth - 1)
        val pixelTop = (top * canvasHeight).roundToInt().coerceIn(0, canvasHeight - 1)
        val pixelRight = ((left + width) * canvasWidth).roundToInt().coerceIn(pixelLeft + 1, canvasWidth)
        val pixelBottom = ((top + height) * canvasHeight).roundToInt().coerceIn(pixelTop + 1, canvasHeight)
        return PixelRect(pixelLeft, pixelTop, pixelRight - pixelLeft, pixelBottom - pixelTop)
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
        const val MAX_GRID_WIDTH_PX = 2048
        const val MIN_PANEL_GAP_PX = 8
    }
}

