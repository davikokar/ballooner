package com.ballooner.ui.project

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * A few custom Material-style icons built from path data, since the project only
 * depends on `material-icons-core` (which lacks image / rotate / save glyphs).
 */
object BalloonerIcons {

    val Image: ImageVector by lazy {
        icon(
            name = "Image",
            pathData = "M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 " +
                "2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z",
        )
    }

    val ImageAdd: ImageVector by lazy {
        icon(
            name = "Add image",
            pathData = "M15 3H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h9v-2H4V5h11v8h2V5c0-1.1-.9-2-2-2z" +
                "M5 15h9l-3-4-2.25 3L7 12l-2 3zm14 0v3h-3v2h3v3h2v-3h3v-2h-3v-3h-2z",
        )
    }

    val Rotate: ImageVector by lazy {
        icon(
            name = "Rotate",
            pathData = "M15.55 5.55L11 1v3.07C7.06 4.56 4 7.92 4 12s3.05 7.44 7 7.93v-2.02c-2.84-.48-5-2.94-5-5.91s2." +
                "16-5.43 5-5.91V10l4.55-4.45zM19.93 11c-.17-1.39-.72-2.73-1.62-3.89l-1.42 1.42c.54.75.88 1.6 " +
                "1.02 2.47h2.02zM13 17.9v2.02c1.39-.17 2.74-.71 3.9-1.61l-1.44-1.44c-.75.54-1.59.89-2.46 1.03zm3." +
                "89-2.42l1.42 1.41c.9-1.16 1.45-2.5 1.62-3.89h-2.02c-.14.87-.48 1.72-1.02 2.48z",
        )
    }

    val Save: ImageVector by lazy {
        icon(
            name = "Save",
            pathData = "M17 3H5c-1.11 0-2 .9-2 2v14c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V7l-4-4zm-5 16c-1.66 0-3-1.34-3-3s1." +
                "34-3 3-3 3 1.34 3 3-1.34 3-3 3zm3-10H5V5h10v4z",
        )
    }

    val FocusImage: ImageVector by lazy {
        icon(
            name = "Focus image",
            pathData = "M3 3v6h2V5h4V3H3zm2 12H3v6h6v-2H5v-4zm14 4h-4v2h6v-6h-2v4zm0-10h2V3h-6v2h4v4z",
        )
    }

    val Move: ImageVector by lazy {
        icon(
            name = "Move",
            pathData = "M10 9h4V6h3l-5-5-5 5h3v3zM9 10H6V7l-5 5 5 5v-3h3v-4zm14 2-5-5v3h-3v4h3v3l5-5zm-9 3h-4v3H7l5 5 5-5h-3v-3z",
        )
    }

    val Resize: ImageVector by lazy {
        icon(
            name = "Resize",
            pathData = "M21 15v6h-6v-2h2.59L6.41 7.83V10H4V4h6v2H7.83L19 17.17V15h2z",
        )
    }

    val Balloon: ImageVector by lazy {
        icon(
            name = "Balloon",
            pathData = "M3,10 a9,7 0 1,0 18,0 a9,7 0 1,0 -18,0 Z M9,15 L7,21 L14,16 Z",
        )
    }

    private fun icon(name: String, pathData: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(
            pathData = PathParser().parsePathString(pathData).toNodes(),
            fill = SolidColor(Color.Black),
        ).build()
}
