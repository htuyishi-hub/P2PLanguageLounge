package tech.isano.language.core.engine.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import tech.isano.language.core.engine.jami.JamiDaemon
import java.io.File
import java.util.UUID

/**
 * Decentralized Voice Note Manager for the 'Chat' (Community) Walkie-Talkie Tab.
 *
 * ZERO-SERVER AUDIO ARCHITECTURE:
 * Replaces centralized audio upload servers (e.g. S3/Firebase) with direct P2P data transmission.
 * Audio is recorded locally with speech-optimized low-bitrate compression, base64-encoded,
 * and broadcast across GNU Jami's OpenDHT / WebRTC Data Channel directly to peers.
 *
 * Incoming voice note payloads are decoded back into audio files stored in the local cache,
 * with file paths emitted via a reactive [SharedFlow] for the UI waveform player.
 */
class P2PVoiceNoteManager(
    private val context: Context,
    private val jamiDaemon: JamiDaemon,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

    private val _incomingVoiceNotes = MutableSharedFlow<String>(replay = 10, extraBufferCapacity = 64)
    /**
     * Emits the absolute file path of newly arrived and reconstructed voice notes.
     */
    val incomingVoiceNotes: SharedFlow<String> = _incomingVoiceNotes.asSharedFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording: Boolean = false

    init {
        // Automatically listen for incoming data channel payloads from Jami daemon
        coroutineScope.launch {
            jamiDaemon.incomingCanvasStrokes.collect { payload ->
                processIncomingPayload(payload)
            }
        }
    }

    /**
     * Initializes the Android [MediaRecorder] with speech-optimized low-bitrate settings.
     */
    fun startRecording(outputFile: File) {
        if (isRecording) {
            stopRecordingSafely()
        }

        try {
            outputFile.parentFile?.let { parent ->
                if (!parent.exists()) parent.mkdirs()
            }

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                // Speech-optimized compression for P2P data channels
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(AUDIO_BITRATE_BPS)
                setAudioSamplingRate(AUDIO_SAMPLE_RATE_HZ)
                setAudioChannels(1) // Mono for voice
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            isRecording = true
        } catch (e: Exception) {
            isRecording = false
            try {
                mediaRecorder?.release()
            } catch (_: Exception) {}
            mediaRecorder = null
            throw e
        }
    }

    /**
     * Stops the [MediaRecorder], converts the raw audio file to a Base64 string,
     * wraps it in a typed JSON envelope, and broadcasts it across Jami's data channel.
     */
    fun stopRecordingAndBroadcast(outputFile: File) {
        stopRecordingSafely()

        if (!outputFile.exists() || outputFile.length() == 0L) {
            return
        }

        val audioBytes = outputFile.readBytes()
        val base64String = Base64Compat.encodeToString(audioBytes)

        val payload = JSONObject().apply {
            put("type", TYPE_VOICE_NOTE)
            put("data", base64String)
            put("timestamp", System.currentTimeMillis())
            put("byteSize", audioBytes.size)
        }.toString()

        jamiDaemon.sendDataChannelPayload(payload)
    }

    /**
     * Parses an incoming data channel payload. If it's a [TYPE_VOICE_NOTE],
     * decodes the Base64 audio string into a local cache file and emits its path.
     */
    fun processIncomingPayload(payload: String) {
        try {
            val json = JSONObject(payload)
            val type = json.optString("type")

            if (type == TYPE_VOICE_NOTE) {
                val base64Data = json.getString("data")
                val audioBytes = Base64Compat.decode(base64Data)

                val tempFile = File(
                    context.cacheDir,
                    "vn_p2p_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.m4a"
                )
                tempFile.writeBytes(audioBytes)

                coroutineScope.launch {
                    _incomingVoiceNotes.emit(tempFile.absolutePath)
                }
            }
        } catch (_: Exception) {
            // Ignore malformed or unrelated data channel messages
        }
    }

    private fun stopRecordingSafely() {
        try {
            if (isRecording) {
                mediaRecorder?.stop()
            }
        } catch (_: Exception) {
            // Ignore brief tap or premature stop
        } finally {
            try {
                mediaRecorder?.release()
            } catch (_: Exception) {}
            mediaRecorder = null
            isRecording = false
        }
    }

    companion object {
        const val TYPE_VOICE_NOTE = "VOICE_NOTE"
        private const val AUDIO_BITRATE_BPS = 24_000 // 24 kbps speech compression
        private const val AUDIO_SAMPLE_RATE_HZ = 16_000 // 16 kHz wideband speech
    }
}

/**
 * Dual-runtime Base64 helper ensuring unit tests run on JVM without Android mock stubs.
 */
internal object Base64Compat {
    fun encodeToString(bytes: ByteArray): String {
        return try {
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (_: Throwable) {
            java.util.Base64.getEncoder().encodeToString(bytes)
        }
    }

    fun decode(str: String): ByteArray {
        return try {
            android.util.Base64.decode(str, android.util.Base64.DEFAULT)
        } catch (_: Throwable) {
            java.util.Base64.getDecoder().decode(str)
        }
    }
}
