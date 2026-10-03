package com.ballooner.ui.comic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.rememberTextMeasurer
import com.ballooner.domain.comic.Comic

/**
 * A comic at a size of someone else's choosing, cropped to fit it.
 *
 * It is the same drawing the editor and the export use, so a comic cannot be listed as something
 * it is not. The page fills the space and whatever overflows is cut off, so every comic in a list
 * is the same size whatever shape its page happens to be.
 */
@Composable
fun ComicThumbnail(comic: Comic, images: PanelImageSource, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    Box(modifier = modifier.clipToBounds()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val viewport = pageCoverViewport(size, comic.pageHeight)
            if (viewport.scale <= 0f) return@Canvas
            drawComicPage(
                comic = comic,
                images = images,
                viewport = viewport,
                textMeasurer = textMeasurer,
            )
        }
    }
}
