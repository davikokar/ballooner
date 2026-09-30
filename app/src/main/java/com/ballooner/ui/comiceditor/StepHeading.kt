package com.ballooner.ui.comiceditor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The heading a step of the editor wears.
 *
 * Every step uses this rather than placing a title of its own, so the heading stays exactly where
 * it is as the user moves between steps. It is a fixed height, because what it carries at its end
 * comes and goes and a heading that resized around it would not hold still either. Place it in a
 * container that does not pad it: the heading owns where it sits.
 */
@Composable
internal fun StepHeading(text: String, trailing: @Composable BoxScope.() -> Unit = {}) {
    HeadingRow(trailing) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The Layout step's heading, which carries the picker's own heading forward so the step reads as
 * one place the user is inside. The leading part goes back to the picker.
 */
@Composable
internal fun LayoutOptionBreadcrumb(current: String, onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    HeadingRow {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = PRESET_PICKER_TITLE,
                style = MaterialTheme.typography.labelLarge,
                color = scheme.primary,
                modifier = Modifier.clickable(onClick = onBack),
            )
            Text("/", style = MaterialTheme.typography.labelLarge, color = scheme.outline)
            Text(current, style = MaterialTheme.typography.labelLarge, color = scheme.onSurface)
        }
    }
}

@Composable
private fun HeadingRow(
    trailing: @Composable BoxScope.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth().height(HEADING_HEIGHT)) {
        Box(modifier = Modifier.align(Alignment.CenterStart).padding(start = HEADING_INSET)) {
            content()
        }
        trailing()
    }
}

private val HEADING_HEIGHT = 44.dp
private val HEADING_INSET = 16.dp
