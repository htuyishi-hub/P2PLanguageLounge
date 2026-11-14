package tech.isano.language.feature.matchmaking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.isano.language.core.engine.model.Peer
import tech.isano.language.core.engine.network.P2PConnectionState
import tech.isano.language.core.engine.network.P2PManager

class MatchmakingViewModel(
    private val p2pManager: P2PManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MatchmakingUiState())
    val uiState: StateFlow<MatchmakingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            p2pManager.connectionState.collect { connState ->
                _uiState.update { it.copy(connectionState = connState) }
            }
        }
        viewModelScope.launch {
            p2pManager.discoveredPeers.collect { peerList ->
                _uiState.update { it.copy(peers = peerList) }
            }
        }
    }

    fun startScanning() {
        _uiState.update { it.copy(isScanning = true) }
        viewModelScope.launch {
            p2pManager.startPeerDiscovery()
        }
    }

    fun stopScanning() {
        _uiState.update { it.copy(isScanning = false) }
        viewModelScope.launch {
            p2pManager.stopPeerDiscovery()
        }
    }

    fun updateLanguages(native: String, target: String) {
        _uiState.update {
            it.copy(myNativeLanguage = native, myTargetLanguage = target)
        }
    }

    fun connectToPeer(peer: Peer, onConnected: () -> Unit) {
        _uiState.update { it.copy(selectedPeer = peer) }
        viewModelScope.launch {
            p2pManager.connectToPeer(peer)
            onConnected()
        }
    }
}
