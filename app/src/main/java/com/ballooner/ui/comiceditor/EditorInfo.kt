package com.ballooner.ui.comiceditor

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.davide.seddio.ballooner.R

/** Everything the editor can be showing, each with its own explanation of what is on it. */
internal enum class EditorView(@StringRes val info: Int) {
    PRESET_PICKER(R.string.info_preset_picker),
    SINGLE(R.string.info_single),
    STRIP(R.string.info_strip),
    GRID(R.string.info_grid),
    CUSTOM(R.string.info_custom),
    IMAGES(R.string.info_images),
    BALLOONS(R.string.info_balloons),
}

internal fun ComicEditorUiState.Content.currentView(): EditorView = when (step) {
    EditorStep.LAYOUT -> when (layoutKind) {
        null -> EditorView.PRESET_PICKER
        LayoutKind.SINGLE -> EditorView.SINGLE
        LayoutKind.STRIP -> EditorView.STRIP
        LayoutKind.GRID -> EditorView.GRID
        LayoutKind.CUSTOM -> EditorView.CUSTOM
    }
    EditorStep.PLACEMENT -> EditorView.IMAGES
    EditorStep.BALLOONS -> EditorView.BALLOONS
}

/** What can be done in [view] and what its buttons mean; the first line says what it is for. */
@Composable
internal fun EditorInfoDialog(view: EditorView, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.screen_info)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                stringResource(view.info).split("\n").forEachIndexed { index, line ->
                    Text(
                        text = line,
                        style = if (index == 0) {
                            MaterialTheme.typography.titleSmall
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}
