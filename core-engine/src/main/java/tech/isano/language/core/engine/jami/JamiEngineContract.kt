package tech.isano.language.core.engine.jami

import android.content.Context
import android.view.SurfaceView
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * OpenDHT cryptographic identity for P2P communication.
 * The archive byte array is designed for zero-knowledge backup via Google Drive / Credential Manager.
 */
data class JamiAccount(
    val accountId: String,
    val archiveBytes: ByteArray,
    val dhtFingerprint: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as JamiAccount
        if (accountId != other.accountId) return false
        if (!archiveBytes.contentEquals(other.archiveBytes)) return false
        if (dhtFingerprint != other.dhtFingerprint) return false
        return true
    }

    override fun hashCode(): Int {
        var result = accountId.hashCode()
        result = 31 * result + archiveBytes.contentHashCode()
        result = 31 * result + dhtFingerprint.hashCode()
        return result
    }
}

/**
 * Reactive states representing Jami connection and Swarm lifecycle.
 */
sealed interface CallState {
    data object Idle : CallState
    data object Connecting : CallState
    data class ActiveSwarm(
        val swarmId: String,
        val peerCount: Int = 1
    ) : CallState
    data class Disconnected(val reason: String? = null) : CallState
}

/**
 * Clean Kotlin interface wrapping native GNU Jami (libjami) calls.
 * Decouples UI ViewModels and Jetpack Compose screens from native JNI complexity.
 *
 * Live Lounge uses Jami Swarms (distributed OpenDHT group conversations) for zero-server
 * auto-drop-in matchmaking and WebRTC/DataChannel synchronization.
 */
interface JamiDaemon {
    /**
     * Observable state flow representing current call/swarm connectivity.
     */
    val callState: StateFlow<CallState>

    /**
     * Shared flow emitting real-time canvas strokes or data channel payloads received from peers.
     */
    val incomingCanvasStrokes: SharedFlow<String>

    /**
     * Shared flow emitting absolute file paths of images/files transferred over Jami Swarm channels.
     */
    val incomingFiles: SharedFlow<String>

    /**
     * Initializes the native Jami daemon runtime with application context.
     */
    fun initDaemon(context: Context)

    /**
     * Generates a new OpenDHT identity and returns the encrypted archive bytes
     * for silent cloud backup.
     */
    fun createAccount(): JamiAccount

    /**
     * Hashes the language pair into a deterministic Swarm ID and joins the distributed DHT group.
     */
    fun joinLanguageSwarm(nativeLang: String, targetLang: String)

    /**
     * Leaves the currently joined language swarm and tears down active data channels.
     */
    fun leaveSwarm()

    /**
     * Broadcasts a Write-on-Screen canvas stroke payload across the active Jami data channel.
     */
    fun sendDataChannelPayload(payload: String)

    /**
     * Binds local and remote Android [SurfaceView] targets to Jami's video pipeline.
     */
    fun bindVideoViews(localView: SurfaceView, remoteView: SurfaceView)

    /**
     * Pushes a local image/file natively to the other user's screen over the Jami Swarm.
     */
    fun sendFileToSwarm(filePath: String)
}
