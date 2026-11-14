package tech.isano.language.core.engine.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import tech.isano.language.core.engine.model.P2PMessage
import tech.isano.language.core.engine.model.Peer

interface P2PManager {
    val connectionState: StateFlow<P2PConnectionState>
    val discoveredPeers: StateFlow<List<Peer>>
    val incomingMessages: SharedFlow<P2PMessage>

    suspend fun startPeerDiscovery()
    suspend fun stopPeerDiscovery()
    suspend fun connectToPeer(peer: Peer)
    suspend fun disconnect()
    suspend fun sendMessage(message: P2PMessage)
}

class InMemoryP2PManager(
    val localUser: Peer = Peer(
        id = "local_peer_001",
        displayName = "Language Explorer",
        nativeLanguage = "English",
        targetLanguage = "Spanish",
        proficiency = "Intermediate"
    )
) : P2PManager {

    private val _connectionState = MutableStateFlow<P2PConnectionState>(P2PConnectionState.Idle)
    override val connectionState: StateFlow<P2PConnectionState> = _connectionState.asStateFlow()

    private val _discoveredPeers = MutableStateFlow<List<Peer>>(emptyList())
    override val discoveredPeers: StateFlow<List<Peer>> = _discoveredPeers.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<P2PMessage>(replay = 50)
    override val incomingMessages: SharedFlow<P2PMessage> = _incomingMessages.asSharedFlow()

    override suspend fun startPeerDiscovery() {
        _connectionState.value = P2PConnectionState.DiscoveringPeers
        // Mock sample nearby discovery peers for testing and initial preview
        _discoveredPeers.value = listOf(
            Peer("peer_101", "Elena Morales", "Spanish", "English", "Advanced", avatarColor = 0xFFEC4899),
            Peer("peer_102", "Kenji Sato", "Japanese", "English", "Intermediate", avatarColor = 0xFFF59E0B),
            Peer("peer_103", "Marie Dubois", "French", "English", "Native", avatarColor = 0xFF3B82F6),
            Peer("peer_104", "Lucas Silva", "Portuguese", "Spanish", "Beginner", avatarColor = 0xFF10B981)
        )
    }

    override suspend fun stopPeerDiscovery() {
        if (_connectionState.value is P2PConnectionState.DiscoveringPeers) {
            _connectionState.value = P2PConnectionState.Idle
        }
    }

    override suspend fun connectToPeer(peer: Peer) {
        _connectionState.value = P2PConnectionState.Connecting(peer)
        _connectionState.value = P2PConnectionState.Connected(listOf(localUser, peer))
        _incomingMessages.emit(
            P2PMessage.Handshake(
                senderId = peer.id,
                peerInfo = peer
            )
        )
    }

    override suspend fun disconnect() {
        _connectionState.value = P2PConnectionState.Disconnected("Disconnected by user")
        _connectionState.value = P2PConnectionState.Idle
    }

    override suspend fun sendMessage(message: P2PMessage) {
        _incomingMessages.emit(message)
    }
}
