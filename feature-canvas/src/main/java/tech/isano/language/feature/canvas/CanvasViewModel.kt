package tech.isano.language.feature.canvas

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.isano.language.core.engine.model.P2PMessage
import tech.isano.language.core.engine.network.P2PManager
import tech.isano.language.feature.canvas.model.DrawPath
import tech.isano.language.feature.canvas.model.DrawPoint
import java.util.UUID

data class CanvasUiState(
    val paths: List<DrawPath> = emptyList(),
    val currentPath: DrawPath? = null,
    val currentColor: Color = Color(0xFF4F46E5),
    val strokeWidth: Float = 6f,
    val connectedPeerCount: Int = 1
)

class CanvasViewModel(
    private val p2pManager: P2PManager,
    private val localPeerId: String = "local_peer_001"
) : ViewModel() {

    private val _uiState = MutableStateFlow(CanvasUiState())
    val uiState: StateFlow<CanvasUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            p2pManager.incomingMessages.collect { msg ->
                when (msg) {
                    is P2PMessage.CanvasStroke -> {
                        val remotePoints = msg.points.chunked(2).mapNotNull {
                            if (it.size == 2) DrawPoint(it[0], it[1]) else null
                        }
                        val remotePath = DrawPath(
                            id = msg.strokeId,
                            peerId = msg.senderId,
                            points = remotePoints,
                            color = Color(msg.colorArgb),
                            strokeWidth = msg.strokeWidth
                        )
                        _uiState.update { it.copy(paths = it.paths + remotePath) }
                    }
                    is P2PMessage.CanvasClear -> {
                        _uiState.update { it.copy(paths = emptyList(), currentPath = null) }
                    }
                    else -> Unit
                }
            }
        }
    }

    fun startStroke(startPoint: DrawPoint) {
        val newPath = DrawPath(
            id = UUID.randomUUID().toString(),
            peerId = localPeerId,
            points = listOf(startPoint),
            color = _uiState.value.currentColor,
            strokeWidth = _uiState.value.strokeWidth
        )
        _uiState.update { it.copy(currentPath = newPath) }
    }

    fun appendPoint(point: DrawPoint) {
        val current = _uiState.value.currentPath ?: return
        val updated = current.copy(points = current.points + point)
        _uiState.update { it.copy(currentPath = updated) }
    }

    fun endStroke() {
        val completed = _uiState.value.currentPath ?: return
        _uiState.update {
            it.copy(
                paths = it.paths + completed,
                currentPath = null
            )
        }

        // Broadcast to P2P peers
        viewModelScope.launch {
            val flatPoints = completed.points.flatMap { listOf(it.x, it.y) }
            val colorLong = completed.color.value.toLong()
            p2pManager.sendMessage(
                P2PMessage.CanvasStroke(
                    senderId = localPeerId,
                    strokeId = completed.id,
                    points = flatPoints,
                    colorArgb = colorLong,
                    strokeWidth = completed.strokeWidth
                )
            )
        }
    }

    fun undo() {
        _uiState.update { state ->
            if (state.paths.isNotEmpty()) {
                state.copy(paths = state.paths.dropLast(1))
            } else state
        }
    }

    fun clearCanvas() {
        _uiState.update { it.copy(paths = emptyList(), currentPath = null) }
        viewModelScope.launch {
            p2pManager.sendMessage(P2PMessage.CanvasClear(senderId = localPeerId))
        }
    }

    fun selectColor(color: Color) {
        _uiState.update { it.copy(currentColor = color) }
    }

    fun selectStrokeWidth(width: Float) {
        _uiState.update { it.copy(strokeWidth = width) }
    }
}
