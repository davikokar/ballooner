package com.ballooner.data.image

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

class AppImageStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : ImageStore {

    private val imagesDir = File(context.filesDir, "images")

    /**
     * Copies the picked file across byte for byte.
     *
     * Decoding and re-encoding here used to be the slowest thing between picking an image and
     * seeing it in its panel, and it cost detail for nothing: every reader already decodes at the
     * size it needs, and the orientation note the file carries travels with the copy.
     */
    override suspend fun importImage(sourceUri: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            imagesDir.mkdirs()
            val destination = File(imagesDir, "img_${UUID.randomUUID()}")
            context.contentResolver.openInputStream(Uri.parse(sourceUri))?.use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            } ?: return@runCatching null
            Uri.fromFile(destination).toString()
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
}
