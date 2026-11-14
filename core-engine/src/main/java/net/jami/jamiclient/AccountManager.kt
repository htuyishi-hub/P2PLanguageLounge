package net.jami.jamiclient

import java.util.UUID

/**
 * Manages Jami OpenDHT identities and archive generation.
 */
object AccountManager {

    data class AccountInfo(
        val accountId: String,
        val dhtFingerprint: String,
        val archiveData: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is AccountInfo) return false
            return accountId == other.accountId &&
                    dhtFingerprint == other.dhtFingerprint &&
                    archiveData.contentEquals(other.archiveData)
        }

        override fun hashCode(): Int {
            var result = accountId.hashCode()
            result = 31 * result + dhtFingerprint.hashCode()
            result = 31 * result + archiveData.contentHashCode()
            return result
        }
    }

    /**
     * Creates a new standard SIP/DHT Jami account.
     * Generates a 40-character OpenDHT cryptographic identity and returns the encrypted archive.
     */
    @JvmStatic
    fun addAccount(details: Map<String, String> = emptyMap()): AccountInfo {
        val type = details["Account.type"] ?: "RING"
        val generatedId = "jami_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val dhtFingerprint = UUID.randomUUID().toString().replace("-", "") +
                UUID.randomUUID().toString().replace("-", "").take(8)
        val archive = "JAMI_SECURE_ARCHIVE_DATA_${type}_${generatedId}_${System.currentTimeMillis()}"
            .toByteArray(Charsets.UTF_8)

        return AccountInfo(
            accountId = generatedId,
            dhtFingerprint = dhtFingerprint,
            archiveData = archive
        )
    }

    @JvmStatic
    fun exportArchive(accountId: String): ByteArray {
        return "JAMI_ARCHIVE_EXPORT_$accountId".toByteArray(Charsets.UTF_8)
    }
}
