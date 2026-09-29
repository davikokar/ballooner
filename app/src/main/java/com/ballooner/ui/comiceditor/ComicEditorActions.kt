package com.ballooner.ui.comiceditor

import com.ballooner.domain.comic.ComicStyle
import com.ballooner.domain.comic.CutScope
import com.ballooner.domain.comic.GridAxis
import com.ballooner.domain.comic.NormalizedPoint
import com.ballooner.domain.comic.PageSizing
import com.ballooner.domain.comic.Span
import com.ballooner.domain.model.BalloonFont
import com.ballooner.domain.model.BalloonType

/** What the editor screen can ask of the editor, so the screen itself stays stateless. */
interface ComicEditorActions {
    fun selectStep(step: EditorStep)
    fun selectTool(tool: LayoutTool)
    fun toggleSelection(span: Span)
    fun mergeSelection()
    fun unmergeSelection()
    fun applyPreset(rows: Int, columns: Int)
    fun selectLayoutKind(kind: LayoutKind)
    fun closeLayoutKind()
    fun setSizing(sizing: PageSizing)
    fun setStyle(style: ComicStyle)
    fun startBoundaryDrag()
    fun moveBoundary(axis: GridAxis, index: Int, delta: Float)
    fun endBoundaryDrag()
    fun moveCutEnd(index: Int, start: Boolean, to: NormalizedPoint)
    fun endCutDrag()
    fun addCut(from: NormalizedPoint, to: NormalizedPoint, scope: CutScope)
    fun selectPanel(index: Int?)
    fun focusPanel(index: Int?)
    fun focusNeighbour(forward: Boolean)
    fun swapPanelImages(from: Int, to: Int)
    fun setPanelImage(index: Int, sourceUri: String?)
    fun startPlacementGesture()
    fun transformPanelImage(
        index: Int,
        imageAspect: Float,
        panX: Float,
        panY: Float,
        zoomBy: Float,
        rotateBy: Float,
    )
    fun endPlacementGesture(index: Int, imageAspect: Float)
    fun addBalloon(type: BalloonType, panelIndex: Int?)
    fun selectBalloon(id: Long?)
    fun deleteBalloon(id: Long)
    fun setBalloonText(id: Long, text: String)
    fun setBalloonFont(id: Long, font: BalloonFont)
    fun setBalloonTextSize(id: Long, size: Float)
    fun setBalloonRoundness(id: Long, roundness: Float)
    fun setBalloonTailWidth(id: Long, x: Float, y: Float)
    fun moveBalloon(id: Long, x: Float, y: Float)
    fun resizeBalloon(id: Long, x: Float, y: Float)
    fun pointBalloonTail(id: Long, x: Float, y: Float)
    fun toggleBalloonScope(id: Long)
    fun startBalloonGesture()
    fun endBalloonGesture()
    fun confirmLayoutChange()
    fun cancelLayoutChange()
    fun undo()
}

fun ComicEditorViewModel.asActions(): ComicEditorActions = object : ComicEditorActions {
    override fun selectStep(step: EditorStep) = this@asActions.selectStep(step)
    override fun selectTool(tool: LayoutTool) = this@asActions.selectTool(tool)
    override fun toggleSelection(span: Span) = this@asActions.toggleSelection(span)
    override fun mergeSelection() = this@asActions.mergeSelection()
    override fun unmergeSelection() = this@asActions.unmergeSelection()
    override fun applyPreset(rows: Int, columns: Int) = this@asActions.applyPreset(rows, columns)
    override fun selectLayoutKind(kind: LayoutKind) = this@asActions.selectLayoutKind(kind)
    override fun closeLayoutKind() = this@asActions.closeLayoutKind()
    override fun setSizing(sizing: PageSizing) = this@asActions.setSizing(sizing)
    override fun setStyle(style: ComicStyle) = this@asActions.setStyle(style)
    override fun startBoundaryDrag() = this@asActions.startBoundaryDrag()
    override fun moveBoundary(axis: GridAxis, index: Int, delta: Float) =
        this@asActions.moveBoundary(axis, index, delta)
    override fun endBoundaryDrag() = this@asActions.endBoundaryDrag()
    override fun moveCutEnd(index: Int, start: Boolean, to: NormalizedPoint) =
        this@asActions.moveCutEnd(index, start, to)
    override fun endCutDrag() = this@asActions.endCutDrag()
    override fun addCut(from: NormalizedPoint, to: NormalizedPoint, scope: CutScope) =
        this@asActions.addCut(from, to, scope)
    override fun selectPanel(index: Int?) = this@asActions.selectPanel(index)
    override fun focusPanel(index: Int?) = this@asActions.focusPanel(index)
    override fun focusNeighbour(forward: Boolean) = this@asActions.focusNeighbour(forward)
    override fun swapPanelImages(from: Int, to: Int) = this@asActions.swapPanelImages(from, to)
    override fun setPanelImage(index: Int, sourceUri: String?) =
        this@asActions.setPanelImage(index, sourceUri)
    override fun startPlacementGesture() = this@asActions.startPlacementGesture()
    override fun transformPanelImage(
        index: Int,
        imageAspect: Float,
        panX: Float,
        panY: Float,
        zoomBy: Float,
        rotateBy: Float,
    ) = this@asActions.transformPanelImage(index, imageAspect, panX, panY, zoomBy, rotateBy)
    override fun endPlacementGesture(index: Int, imageAspect: Float) =
        this@asActions.endPlacementGesture(index, imageAspect)
    override fun addBalloon(type: BalloonType, panelIndex: Int?) =
        this@asActions.addBalloon(type, panelIndex)
    override fun selectBalloon(id: Long?) = this@asActions.selectBalloon(id)
    override fun deleteBalloon(id: Long) = this@asActions.deleteBalloon(id)
    override fun setBalloonText(id: Long, text: String) = this@asActions.setBalloonText(id, text)
    override fun setBalloonFont(id: Long, font: BalloonFont) = this@asActions.setBalloonFont(id, font)
    override fun setBalloonTextSize(id: Long, size: Float) = this@asActions.setBalloonTextSize(id, size)
    override fun setBalloonRoundness(id: Long, roundness: Float) =
        this@asActions.setBalloonRoundness(id, roundness)
    override fun setBalloonTailWidth(id: Long, x: Float, y: Float) =
        this@asActions.setBalloonTailWidth(id, x, y)
    override fun moveBalloon(id: Long, x: Float, y: Float) = this@asActions.moveBalloon(id, x, y)
    override fun resizeBalloon(id: Long, x: Float, y: Float) = this@asActions.resizeBalloon(id, x, y)
    override fun pointBalloonTail(id: Long, x: Float, y: Float) =
        this@asActions.pointBalloonTail(id, x, y)
    override fun toggleBalloonScope(id: Long) = this@asActions.toggleBalloonScope(id)
    override fun startBalloonGesture() = this@asActions.startBalloonGesture()
    override fun endBalloonGesture() = this@asActions.endBalloonGesture()
    override fun confirmLayoutChange() = this@asActions.confirmLayoutChange()
    override fun cancelLayoutChange() = this@asActions.cancelLayoutChange()
    override fun undo() = this@asActions.undo()
}
