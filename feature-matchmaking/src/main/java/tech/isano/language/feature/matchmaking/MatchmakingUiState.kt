package tech.isano.language.feature.matchmaking

import tech.isano.language.core.engine.model.Peer
import tech.isano.language.core.engine.network.P2PConnectionState

data class MatchmakingUiState(
    val myNativeLanguage: String = "English",
    val myTargetLanguage: String = "Spanish",
    val isScanning: Boolean = false,
    val peers: List<Peer> = emptyList(),
    val connectionState: P2PConnectionState = P2PConnectionState.Idle,
    val selectedPeer: Peer? = null
)
