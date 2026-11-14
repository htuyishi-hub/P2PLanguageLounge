package tech.isano.language.feature.canvas.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

data class DrawPoint(
    val x: Float,
    val y: Float
) {
    fun toOffset(): Offset = Offset(x, y)
}

data class DrawPath(
    val id: String,
    val peerId: String,
    val points: List<DrawPoint>,
    val color: Color,
    val strokeWidth: Float = 6f
)
