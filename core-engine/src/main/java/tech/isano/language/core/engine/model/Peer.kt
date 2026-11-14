package tech.isano.language.core.engine.model

data class Peer(
    val id: String,
    val displayName: String,
    val nativeLanguage: String,
    val targetLanguage: String,
    val proficiency: String = "Intermediate",
    val isHost: Boolean = false,
    val avatarColor: Long = 0xFF4F46E5
)
