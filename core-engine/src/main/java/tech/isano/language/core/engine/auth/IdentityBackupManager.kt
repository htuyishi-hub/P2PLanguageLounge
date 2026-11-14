package tech.isano.language.core.engine.auth

import android.content.Context

/**
 * Silent, friction-free identity backup bridge.
 *
 * UX MANDATE:
 * Completely bans 12-word recovery seed phrases, 40-character hex keys, and manual export files.
 * The OpenDHT Jami key archive is silently and securely backed up to and restored from
 * the hidden `appDataFolder` on Google Drive upon Google One-Tap authentication.
 *
 * Keeps the application 100% zero-server and free forever.
 */
interface IdentityBackupManager {
    /**
     * Authenticates the user via AndroidX Credential Manager (Google One-Tap).
     * Returns the Google account email or ID.
     */
    suspend fun signInWithGoogle(activityContext: Context): String

    /**
     * Uploads the raw Jami OpenDHT key archive to Google Drive's hidden `appDataFolder`
     * as `jami_backup.dat`. Returns true if backup succeeded.
     */
    suspend fun backupJamiKeyToDrive(keyArchive: ByteArray): Boolean

    /**
     * Queries the hidden `appDataFolder` on Google Drive for `jami_backup.dat`
     * and downloads the key archive if available.
     * Returns null if no prior backup exists.
     */
    suspend fun restoreJamiKeyFromDrive(): ByteArray?
}
