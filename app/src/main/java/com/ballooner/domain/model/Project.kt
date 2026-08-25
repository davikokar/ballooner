package com.ballooner.domain.model

/**
 * A comic project: a named collection of images with balloons added to them.
 * Images and balloons are added in later screens; this model captures the
 * project's identity.
 */
data class Project(
    val id: Long,
    val name: String,
    val description: String,
    val createdAt: Long,
    val imageUri: String? = null,
)
