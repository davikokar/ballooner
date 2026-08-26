package com.ballooner.data.image

import android.content.Context
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
}
