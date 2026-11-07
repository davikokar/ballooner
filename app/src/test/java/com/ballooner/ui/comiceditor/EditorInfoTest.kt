package com.ballooner.ui.comiceditor

import com.ballooner.domain.comic.Comic
import org.junit.Assert.assertEquals
import org.junit.Test

class EditorInfoTest {

    private fun content(step: EditorStep, kind: LayoutKind? = null) =
        ComicEditorUiState.Content(comic = Comic(), step = step, layoutKind = kind)

    @Test
    fun `the preset picker is the Layout step with no kind open`() {
        assertEquals(EditorView.PRESET_PICKER, content(EditorStep.LAYOUT).currentView())
    }

    @Test
    fun `each layout kind explains its own options`() {
        val views = mapOf(
            LayoutKind.SINGLE to EditorView.SINGLE,
            LayoutKind.STRIP to EditorView.STRIP,
            LayoutKind.GRID to EditorView.GRID,
            LayoutKind.CUSTOM to EditorView.CUSTOM,
        )

        views.forEach { (kind, view) ->
            assertEquals(view, content(EditorStep.LAYOUT, kind).currentView())
        }
    }

    @Test
    fun `the Images and Balloons steps have their own views`() {
        assertEquals(EditorView.IMAGES, content(EditorStep.PLACEMENT).currentView())
        assertEquals(EditorView.BALLOONS, content(EditorStep.BALLOONS).currentView())
    }

    @Test
    fun `no two views share an explanation`() {
        assertEquals(EditorView.entries.size, EditorView.entries.map { it.info }.toSet().size)
    }
}
