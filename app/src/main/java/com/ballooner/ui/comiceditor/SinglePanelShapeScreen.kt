package com.ballooner.ui.comiceditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ballooner.domain.comic.ComicStyle
import com.ballooner.domain.comic.PageSizing
import com.ballooner.domain.comic.Panel
import com.ballooner.ui.comic.PanelImageSource

/**
 * The Single preset's own screen: the shape of the one panel, which for a single-panel comic is
 * the shape of the whole comic.
 */
@Composable
fun SinglePanelShapeScreen(
    sizing: PageSizing,
    panelRatio: Float,
    panel: Panel?,
    images: PanelImageSource,
    style: ComicStyle,
    expanded: Boolean,
    onExpanded: (Boolean) -> Unit,
    onChange: (PageSizing) -> Unit,
    onBack: () -> Unit,
    onOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!expanded) LayoutOptionBreadcrumb(current = "SINGLE", onBack = onBack, onOptions = onOptions)
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp).padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!expanded) {
                PanelShapeChooser(sizing = sizing, autoCaption = "Fit image", onChange = onChange)
            }
            PanelPreview(
                // The panel as it will really be: a ratio the page cannot reach is held back, and
                // the preview has to say so rather than promise the number on the slider. An auto
                // panel has no shape to show until an image gives it one.
                ratio = panelRatio.takeIf { sizing !is PageSizing.FromImage || panel?.image != null },
                panels = listOfNotNull(panel),
                images = images,
                style = style,
                // No Panel options here: the one panel is the whole comic, so styling it and
                // styling the comic are the same thing and the Options button already does it.
                modifier = Modifier.weight(1f),
                expanded = expanded,
                onExpanded = onExpanded,
            )
        }
    }
}
