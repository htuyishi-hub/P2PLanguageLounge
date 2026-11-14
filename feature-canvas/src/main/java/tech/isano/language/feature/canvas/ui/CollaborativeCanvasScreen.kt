package tech.isano.language.feature.canvas.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import tech.isano.language.feature.canvas.CanvasViewModel
import tech.isano.language.feature.canvas.model.DrawPath
import tech.isano.language.feature.canvas.model.DrawPoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollaborativeCanvasScreen(
    viewModel: CanvasViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Collaborative Whiteboard") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                viewModel.startStroke(DrawPoint(offset.x, offset.y))
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                viewModel.appendPoint(DrawPoint(change.position.x, change.position.y))
                            },
                            onDragEnd = {
                                viewModel.endStroke()
                            },
                            onDragCancel = {
                                viewModel.endStroke()
                            }
                        )
                    }
            ) {
                // Render all confirmed paths
                uiState.paths.forEach { drawPath ->
                    renderPath(drawPath)
                }

                // Render currently active local stroke
                uiState.currentPath?.let { activePath ->
                    renderPath(activePath)
                }
            }

            // Toolbar pinned at bottom
            CanvasToolbar(
                selectedColor = uiState.currentColor,
                onColorSelected = { viewModel.selectColor(it) },
                onUndoClick = { viewModel.undo() },
                onClearClick = { viewModel.clearCanvas() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.renderPath(drawPath: DrawPath) {
    if (drawPath.points.size < 2) return

    val path = Path().apply {
        moveTo(drawPath.points.first().x, drawPath.points.first().y)
        for (i in 1 until drawPath.points.size) {
            lineTo(drawPath.points[i].x, drawPath.points[i].y)
        }
    }

    drawPath(
        path = path,
        color = drawPath.color,
        style = Stroke(
            width = drawPath.strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}
