package tech.isano.language.core.engine.model

enum class SessionMode {
    LOBBY,
    VOICE_EXCHANGE,
    COLLABORATIVE_CANVAS,
    VOCABULARY_FLASHCARDS
}

data class LoungeSession(
    val sessionId: String,
    val title: String,
    val hostPeer: Peer,
    val connectedPeers: List<Peer> = emptyList(),
    val nativeLanguage: String,
    val targetLanguage: String,
    val currentMode: SessionMode = SessionMode.LOBBY,
    val isMicMuted: Boolean = false,
    val isAudioConnected: Boolean = false
)
