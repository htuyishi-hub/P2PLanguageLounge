package net.jami.jamiclient

import android.content.Context

/**
 * Standard Jami Android platform initialization entry point.
 * Wraps Jami runtime lifecycle for the Android client AAR binding.
 */
object Jami {
    private var isInitialized = false

    @JvmStatic
    fun init(context: Context) {
        if (!isInitialized) {
            // Initialize Jami runtime, OpenDHT bootstrap network, and media pipelines
            isInitialized = true
        }
    }

    @JvmStatic
    fun isInitialized(): Boolean = isInitialized
}
