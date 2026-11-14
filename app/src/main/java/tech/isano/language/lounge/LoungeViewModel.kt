package tech.isano.language.lounge

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.isano.language.core.engine.audio.P2PAudioManager
import tech.isano.language.core.engine.jami.CallState
import tech.isano.language.core.engine.jami.JamiDaemon
import tech.isano.language.core.engine.jami.JamiDaemonImpl
import tech.isano.language.core.engine.model.Peer
import tech.isano.language.core.engine.network.InMemoryP2PManager
import tech.isano.language.core.engine.network.P2PManager

/**
 * ViewModel orchestrating the Live Lounge auto-drop-in experience.
 * Bridges Jami OpenDHT Swarm state, P2PAudioManager speakerphone routing,
 * and background peer arrival notifications.
 */
class LoungeViewModel(
    application: Application,
    private val p2pManager: P2PManager = InMemoryP2PManager(),
    private val jamiDaemon: JamiDaemon = JamiDaemonImpl(),
    private val p2pAudioManager: P2PAudioManager = P2PAudioManager(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<LoungeUiState>(LoungeUiState.Idle)
    val uiState: StateFlow<LoungeUiState> = _uiState.asStateFlow()

    private var lobbyJob: Job? = null
    private var backgroundListenerJob: Job? = null

    companion object {
        private const val CHANNEL_ID = "p2p_lounge_channel"
        private const val NOTIFICATION_ID = 1001
    }

    init {
        createNotificationChannel()
        observeJamiCallState()
    }

    /**
     * Observes Jami daemon call events to automatically control loudspeaker routing.
     */
    private fun observeJamiCallState() {
        viewModelScope.launch {
            jamiDaemon.callState.collect { state ->
                when (state) {
                    is CallState.ActiveSwarm -> {
                        p2pAudioManager.setSpeakerphoneOn(true)
                        _uiState.value = LoungeUiState.Connected(state.swarmId)
                    }
                    is CallState.Idle, is CallState.Disconnected -> {
                        p2pAudioManager.setSpeakerphoneOn(false)
                    }
                    is CallState.Connecting -> {
                        _uiState.value = LoungeUiState.Scanning
                    }
                }
            }
        }
    }

    /**
     * Triggers permission primer screen if mic permission is needed.
     */
    fun showPermissionPrimer() {
        _uiState.value = LoungeUiState.PermissionPrimer
    }

    /**
     * Resets back to Idle if permission is denied.
     */
    fun resetFromPrimer() {
        _uiState.value = LoungeUiState.Idle
    }

    /**
     * Drops into the automatic radio frequency / OpenDHT lobby.
     * Eliminates dialing and approach anxiety.
     */
    fun joinLounge(nativeLanguage: String = "English", targetLanguage: String = "Spanish") {
        joinLobby(nativeLanguage, targetLanguage)
    }

    fun joinLobby(nativeLanguage: String = "English", targetLanguage: String = "Spanish") {
        lobbyJob?.cancel()
        backgroundListenerJob?.cancel()

        _uiState.value = LoungeUiState.Scanning

        // Join Jami Swarm over OpenDHT
        jamiDaemon.joinLanguageSwarm(nativeLanguage, targetLanguage)

        lobbyJob = viewModelScope.launch {
            try {
                p2pManager.startPeerDiscovery()

                // Query window to check OpenDHT hash for active peers in this language frequency
                delay(1200)

                val availablePeers = p2pManager.discoveredPeers.value
                val matchedPeer = availablePeers.find { peer ->
                    peer.nativeLanguage.equals(targetLanguage, ignoreCase = true) ||
                    peer.targetLanguage.equals(nativeLanguage, ignoreCase = true)
                } ?: availablePeers.firstOrNull()

                if (matchedPeer != null) {
                    // Peer exists -> instantly bridge into session and activate loudspeaker
                    p2pManager.connectToPeer(matchedPeer)
                    p2pAudioManager.setSpeakerphoneOn(true)
                    _uiState.value = LoungeUiState.Connected(matchedPeer.id)
                } else {
                    // Room is empty -> quiet state, monitor DHT hash in background
                    _uiState.value = LoungeUiState.RoomEmpty_Listening
                    startBackgroundDhtMonitoring(nativeLanguage, targetLanguage)
                }
            } catch (_: Exception) {
                _uiState.value = LoungeUiState.Idle
            }
        }
    }

    private fun startBackgroundDhtMonitoring(nativeLanguage: String, targetLanguage: String) {
        backgroundListenerJob?.cancel()
        backgroundListenerJob = viewModelScope.launch {
            p2pManager.discoveredPeers.collect { peers ->
                if (_uiState.value is LoungeUiState.RoomEmpty_Listening && peers.isNotEmpty()) {
                    val incomingPeer = peers.find { peer ->
                        peer.nativeLanguage.equals(targetLanguage, ignoreCase = true) ||
                        peer.targetLanguage.equals(nativeLanguage, ignoreCase = true)
                    } ?: peers.first()

                    onPeerJoinedBackground(incomingPeer)
                }
            }
        }
    }

    /**
     * Triggered when a peer drops into the shared lobby while the user is in RoomEmpty_Listening.
     */
    fun onPeerJoinedBackground(peer: Peer? = null) {
        val peerId = peer?.id ?: "peer_drop_in"

        // Trigger Android OS Notification
        sendDropInNotification()

        // Instantly bridge into the session with speakerphone active
        viewModelScope.launch {
            peer?.let { p2pManager.connectToPeer(it) }
            p2pAudioManager.setSpeakerphoneOn(true)
            _uiState.value = LoungeUiState.Connected(peerId)
        }
    }

    private fun sendDropInNotification() {
        val context = getApplication<Application>()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("Someone is ready to talk!")
            .setContentText("A language partner just dropped into your lounge.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Language Lounge Drop-In",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when a peer drops into your language lobby"
            }
            val notificationManager = getApplication<Application>().getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun leaveLobby() {
        lobbyJob?.cancel()
        backgroundListenerJob?.cancel()
        lobbyJob = null
        backgroundListenerJob = null
        _uiState.value = LoungeUiState.Idle

        p2pAudioManager.setSpeakerphoneOn(false)
        jamiDaemon.leaveSwarm()

        viewModelScope.launch {
            try {
                p2pManager.stopPeerDiscovery()
                p2pManager.disconnect()
            } catch (_: Exception) {
                // Silent reset
            }
        }
    }
}
