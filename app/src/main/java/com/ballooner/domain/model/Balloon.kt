package com.ballooner.domain.model

/**
 * A single balloon placed on the comic image.
 *
 * All geometry is stored as fractions of the image (0f..1f) so a balloon keeps
 * its relative position and size regardless of how the image is scaled on screen.
 */
data class Balloon(
    val id: Long,
    val type: BalloonType,
    val text: String = "",
    // Center of the balloon as a fraction of the image (0f..1f).
    val centerX: Float = 0.5f,
    val centerY: Float = 0.5f,
    // Balloon body size as a fraction of the image (0f..1f).
    val width: Float = 0.4f,
    val height: Float = 0.25f,
    // Direction the tail points, in degrees clockwise from the positive x-axis.
    val tailAngleDegrees: Float = 90f,
    // How far the tail extends past the body, as a fraction of the image.
    val tailLength: Float = 0.12f,
)
