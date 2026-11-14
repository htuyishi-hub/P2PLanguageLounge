package tech.isano.language.core.engine.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Production [CloudAuthRepository] backed by Supabase PostgREST.
 *
 * Persists and retrieves zero-knowledge encrypted Jami account archives and cryptographic
 * salts over HTTPS using Supabase's PostgREST API with publishable/anon key authorization.
 */
class SupabaseCloudAuthRepository(
    private val supabaseUrl: String = "https://tknesnaeutwyyxrmqryn.supabase.co",
    private val apiKey: String = "sb_publishable_auqy_Fdr39lecuR90DKHGA_jUsXoShS"
) : CloudAuthRepository {

    override suspend fun backupArchive(username: String, salt: String, encryptedArchive: ByteArray) {
        withContext(Dispatchers.IO) {
            val normalized = username.lowercase().trim()
            val endpoint = "$supabaseUrl/rest/v1/user_vaults"
            val url = URL(endpoint)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "application/json")
                // Upsert: merge duplicates if username already exists
                setRequestProperty("Prefer", "resolution=merge-duplicates")
            }

            val payload = JSONObject().apply {
                put("username", normalized)
                put("salt", salt)
                put("encrypted_archive", encryptedArchive.toHex())
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorStream = connection.errorStream ?: connection.inputStream
                val errorBody = errorStream?.bufferedReader()?.use { it.readText() }
                throw IllegalStateException("Supabase backup failed ($responseCode): $errorBody")
            }
        }
    }

    override suspend fun fetchArchive(username: String): Pair<String, ByteArray>? {
        return withContext(Dispatchers.IO) {
            val normalized = username.lowercase().trim()
            val encodedUser = URLEncoder.encode(normalized, "UTF-8")
            val endpoint = "$supabaseUrl/rest/v1/user_vaults?username=eq.$encodedUser&select=salt,encrypted_archive"
            val url = URL(endpoint)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("apikey", apiKey)
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Accept", "application/json")
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                return@withContext null
            }

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(responseText)
            if (jsonArray.length() == 0) {
                return@withContext null
            }

            val obj = jsonArray.getJSONObject(0)
            val salt = obj.getString("salt")
            val archiveHex = obj.getString("encrypted_archive")
            Pair(salt, archiveHex.fromHex())
        }
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
