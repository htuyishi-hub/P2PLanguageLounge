package tech.isano.language.core.engine.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * Dedicated Voice Note Recorder for the Walkie-Talkie Community Chat.
 *
 * Captures speech-optimized AAC audio directly to the application cache directory
 * for immediate peer-to-peer file transfer over Jami Swarms.
 */
class VoiceNoteRecorder(
    private val context: Context? = null,
    private val targetDir: File? = null,
    private val engineFactory: ((File) -> AudioRecorderEngine)? = null
) {

    /**
     * Pluggable audio engine interface enabling testing and decoupling from android.media.MediaRecorder.
     */
    interface AudioRecorderEngine {
        fun prepareAndStart()
        fun stop()
        fun release()
    }

    private var activeEngine: AudioRecorderEngine? = null
    private var currentOutputFile: File? = null
    var isRecording: Boolean = false
        private set

    companion object {
        const val FILE_PREFIX = "voicenote_"
        const val FILE_EXTENSION = ".aac"
        const val SPEECH_BITRATE = 32_000 // 32 kbps speech-optimized
        const val SPEECH_SAMPLE_RATE = 16_000 // 16 kHz wideband speech
    }

    /**
     * Initializes and starts recording an AAC voice note into a new file in [Context.cacheDir] or [targetDir].
     * Returns the output [File] reference.
     */
    fun startRecording(): File {
        if (isRecording) {
            stopRecording()
        }

        val destinationDir = targetDir ?: context?.cacheDir ?: File(System.getProperty("java.io.tmpdir", "/tmp"))
        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
        }

        val outputFile = File(
            destinationDir,
            "$FILE_PREFIX${System.currentTimeMillis()}$FILE_EXTENSION"
        )

        val engine = engineFactory?.invoke(outputFile) ?: createDefaultEngine(outputFile)
        engine.prepareAndStart()

        activeEngine = engine
        currentOutputFile = outputFile
        isRecording = true
        return outputFile
    }

    /**
     * Stops and releases the active recorder engine.
     */
    fun stopRecording() {
        try {
            if (isRecording) {
                activeEngine?.stop()
            }
        } catch (_: Exception) {
            // Handled when user taps and releases too rapidly (< 100ms)
        } finally {
            try {
                activeEngine?.release()
            } catch (_: Exception) {}
            activeEngine = null
            isRecording = false
        }
    }

    /**
     * Returns the output file of the last recorded session.
     */
    fun getCurrentFile(): File? = currentOutputFile

    private fun createDefaultEngine(outputFile: File): AudioRecorderEngine {
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && context != null) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        return object : AudioRecorderEngine {
            override fun prepareAndStart() {
                recorder.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(SPEECH_BITRATE)
                    setAudioSamplingRate(SPEECH_SAMPLE_RATE)
                    setOutputFile(outputFile.absolutePath)
                    prepare()
                    start()
                }
            }

            override fun stop() {
                recorder.stop()
            }

            override fun release() {
                recorder.release()
            }
        }
    }
}
