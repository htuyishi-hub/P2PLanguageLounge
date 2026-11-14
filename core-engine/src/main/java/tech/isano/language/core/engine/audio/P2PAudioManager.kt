package tech.isano.language.core.engine.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

/**
 * Manages device audio routing and speakerphone state for P2P voice calls.
 *
 * Automatically routes audio to the loudspeaker in [AudioManager.MODE_IN_COMMUNICATION]
 * when a user drops into an active Live Lounge room.
 */
class P2PAudioManager(
    private val context: Context
) {

    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /**
     * Enables or disables loudspeaker audio routing for hands-free conversational drop-in.
     * Compatible with Android 12+ CommunicationDevice API and legacy speakerphone routing.
     */
    fun setSpeakerphoneOn(isOn: Boolean) {
        if (isOn) {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val speakerDevice = audioManager.availableCommunicationDevices.find { device ->
                    device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                }
                if (speakerDevice != null) {
                    audioManager.setCommunicationDevice(speakerDevice)
                } else {
                    @Suppress("DEPRECATION")
                    audioManager.isSpeakerphoneOn = true
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = true
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.clearCommunicationDevice()
            }
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = false
            audioManager.mode = AudioManager.MODE_NORMAL
        }
    }
}
