package com.ballooner.data.image

/** Records deletions and echoes imports so tests can assert cleanup. */
class FakeImageStore : ImageStore {
    val deleted = mutableListOf<String>()

    override suspend fun importImage(sourceUri: String): String = sourceUri

    override suspend fun deleteImage(uri: String) {
        deleted += uri
    }
}
