package tech.isano.language.core.engine.model

sealed interface P2PMessage {
    val senderId: String
    val timestamp: Long

    data class Handshake(
        override val senderId: String,
        override val timestamp: Long = System.currentTimeMillis(),
        val peerInfo: Peer
    ) : P2PMessage

    data class TextMessage(
        override val senderId: String,
        override val timestamp: Long = System.currentTimeMillis(),
        val content: String
    ) : P2PMessage

    data class CanvasStroke(
        override val senderId: String,
        override val timestamp: Long = System.currentTimeMillis(),
        val strokeId: String,
        val points: List<Float>,
        val colorArgb: Long,
        val strokeWidth: Float
    ) : P2PMessage

    data class CanvasClear(
        override val senderId: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : P2PMessage

    data class ModeChange(
        override val senderId: String,
        override val timestamp: Long = System.currentTimeMillis(),
        val newMode: SessionMode
    ) : P2PMessage
}
