package tech.isano.language.core.engine.auth

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import tech.isano.language.core.engine.jami.JamiAccount
import tech.isano.language.core.engine.jami.JamiDaemon
import tech.isano.language.core.engine.jami.JamiDaemonImpl

/**
 * Repository interface for sovereign hybrid-cloud identity storage.
 * Stores zero-knowledge encrypted Jami account archives paired with per-user cryptographic salts.
 */
interface CloudAuthRepository {
    suspend fun backupArchive(username: String, salt: String, encryptedArchive: ByteArray)
    suspend fun fetchArchive(username: String): Pair<String, ByteArray>?
}

/**
 * In-memory mock implementation of [CloudAuthRepository].
 * Acts as the zero-knowledge identity escrow service.
 */
class InMemoryCloudAuthRepository : CloudAuthRepository {
    private val storage = mutableMapOf<String, Pair<String, ByteArray>>()

    override suspend fun backupArchive(username: String, salt: String, encryptedArchive: ByteArray) {
        val normalized = username.lowercase().trim()
        storage[normalized] = salt to encryptedArchive
    }

    override suspend fun fetchArchive(username: String): Pair<String, ByteArray>? {
        val normalized = username.lowercase().trim()
        return storage[normalized]
    }

    fun clear() {
        storage.clear()
    }
}

/**
 * Sovereign Hybrid-Cloud Identity Manager.
 *
 * Implements a 4-digit PIN + Salt zero-reading cryptographic pipeline:
 * 1. Derives a 256-bit AES key using PBKDF2WithHmacSHA256 (65,536 iterations).
 * 2. Encrypts/Decrypts the GNU Jami OpenDHT cryptographic identity archive using AES/GCM/NoPadding.
 * 3. Backs up / recovers encrypted blobs to/from [CloudAuthRepository].
 */
class CloudIdentityManager(
    private val repository: CloudAuthRepository = SupabaseCloudAuthRepository(),
    private val jamiDaemon: JamiDaemon = JamiDaemonImpl()
) {

    companion object {
        const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_LENGTH_BITS = 256
        const val PBKDF2_ITERATIONS = 65_536
        const val GCM_TAG_LENGTH_BITS = 128
        const val GCM_IV_LENGTH_BYTES = 12
        const val SALT_LENGTH_BYTES = 16
    }

    private val secureRandom = SecureRandom()

    /**
     * Creates a new Jami account, encrypts its OpenDHT archive with the 4-digit PIN + generated salt,
     * and uploads the ciphertext to the [CloudAuthRepository].
     */
    suspend fun registerAndBackup(username: String, pin: String): JamiAccount {
        val account = jamiDaemon.createAccount()
        val backupSuccess = backupIdentity(username, pin, account.archiveBytes)
        if (!backupSuccess) {
            throw IllegalStateException("Failed to backup identity for user: $username")
        }
        return account
    }

    /**
     * Encrypts the provided [keyArchive] using a key derived from [pin] and a fresh salt,
     * then stores the salt and ciphertext in the repository.
     */
    suspend fun backupIdentity(username: String, pin: String, keyArchive: ByteArray): Boolean {
        return try {
            val saltBytes = ByteArray(SALT_LENGTH_BYTES).apply { secureRandom.nextBytes(this) }
            val saltHex = saltBytes.toHex()

            val secretKey = deriveKey(pin, saltBytes)

            val iv = ByteArray(GCM_IV_LENGTH_BYTES).apply { secureRandom.nextBytes(this) }
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

            val ciphertext = cipher.doFinal(keyArchive)
            val combinedPayload = iv + ciphertext

            repository.backupArchive(username, saltHex, combinedPayload)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Fetches the user's salt and ciphertext, derives the AES key using [pin],
     * and decrypts the Jami key archive.
     * Returns the decrypted [ByteArray] or null if the user does not exist or the PIN is incorrect.
     */
    suspend fun restoreIdentity(username: String, pin: String): ByteArray? {
        val record = repository.fetchArchive(username) ?: return null
        val (saltHex, combinedPayload) = record

        if (combinedPayload.size <= GCM_IV_LENGTH_BYTES) {
            return null
        }

        return try {
            val saltBytes = saltHex.fromHex()
            val secretKey = deriveKey(pin, saltBytes)

            val iv = combinedPayload.copyOfRange(0, GCM_IV_LENGTH_BYTES)
            val ciphertext = combinedPayload.copyOfRange(GCM_IV_LENGTH_BYTES, combinedPayload.size)

            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

            cipher.doFinal(ciphertext)
        } catch (e: Exception) {
            // Bad tag, invalid padding, or wrong PIN -> return null
            null
        }
    }

    /**
     * Unified Sovereign Lockbox Entry:
     * - If username already exists: recovers salt, derives key from [pin], and decrypts archive.
     *   Fails if PIN is incorrect.
     * - If username does NOT exist: generates fresh Jami OpenDHT identity, generates salt, derives key,
     *   encrypts with AES/GCM/NoPadding, and stores in repository.
     *
     * Returns Pair(isSuccess, errorMessage).
     */
    suspend fun lockboxAuth(username: String, pin: String): Pair<Boolean, String?> {
        val normalized = username.lowercase().trim()
        val existing = repository.fetchArchive(normalized)
        return if (existing != null) {
            val decrypted = restoreIdentity(normalized, pin)
            if (decrypted != null) {
                Pair(true, null)
            } else {
                Pair(false, "Incorrect PIN for @$normalized")
            }
        } else {
            try {
                registerAndBackup(normalized, pin)
                Pair(true, null)
            } catch (e: Exception) {
                Pair(false, e.message ?: "Failed to initialize sovereign vault")
            }
        }
    }

    /**
     * Resets or changes the 4-digit PIN for an existing user:
     * 1. Decrypts the sovereign archive using [oldPin].
     * 2. Re-encrypts with [newPin] and a fresh cryptographic salt.
     * 3. Backs up the updated vault to [repository].
     */
    suspend fun changePin(username: String, oldPin: String, newPin: String): Pair<Boolean, String?> {
        val normalized = username.lowercase().trim()
        val archive = restoreIdentity(normalized, oldPin)
            ?: return Pair(false, "Incorrect current PIN. Cannot reset.")
        return try {
            backupIdentity(normalized, newPin, archive)
            Pair(true, null)
        } catch (e: Exception) {
            Pair(false, e.message ?: "Failed to update PIN in vault")
        }
    }

    private fun deriveKey(pin: String, salt: ByteArray): SecretKeySpec {
        val keyFactory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val keySpec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val secretKey = keyFactory.generateSecret(keySpec)
        return SecretKeySpec(secretKey.encoded, "AES")
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.fromHex(): ByteArray {
        val len = length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(this[i], 16) shl 4) + Character.digit(this[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
