package tech.isano.language.feature.canvas.model

import androidx.compose.ui.graphics.Color

sealed interface CanvasAction {
    data class AddPoint(val point: DrawPoint) : CanvasAction
    object EndStroke : CanvasAction
    object Undo : CanvasAction
    object Clear : CanvasAction
    data class ChangeColor(val color: Color) : CanvasAction
    data class ChangeStrokeWidth(val width: Float) : CanvasAction
}
