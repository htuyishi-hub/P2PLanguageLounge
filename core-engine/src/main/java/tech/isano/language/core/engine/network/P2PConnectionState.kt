package tech.isano.language.core.engine.network

import tech.isano.language.core.engine.model.Peer

sealed interface P2PConnectionState {
    object Idle : P2PConnectionState
    object DiscoveringPeers : P2PConnectionState
    data class Connecting(val targetPeer: Peer) : P2PConnectionState
    data class Connected(val sessionPeers: List<Peer>) : P2PConnectionState
    data class Disconnected(val reason: String? = null) : P2PConnectionState
    data class Error(val message: String) : P2PConnectionState
}
