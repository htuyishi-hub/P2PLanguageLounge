package tech.isano.language.core.engine.audio

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class P2PVoiceNoteManagerTest {

    @Test
    fun testBase64EncodingAndDecoding_isLossless() {
        val originalAudioData = "RAW_AAC_OR_OPUS_SAMPLE_AUDIO_BYTES_TEST_STREAM".toByteArray(Charsets.UTF_8)
        val encoded = Base64Compat.encodeToString(originalAudioData)
        val decoded = Base64Compat.decode(encoded)

        assertTrue(originalAudioData.contentEquals(decoded))
    }

    @Test
    fun testVoiceNotePayloadStructure() {
        val sampleBytes = "SAMPLE_VOICE_NOTE_PAYLOAD".toByteArray(Charsets.UTF_8)
        val base64 = Base64Compat.encodeToString(sampleBytes)

        val json = JSONObject().apply {
            put("type", P2PVoiceNoteManager.TYPE_VOICE_NOTE)
            put("data", base64)
            put("timestamp", System.currentTimeMillis())
        }

        assertEquals("VOICE_NOTE", json.getString("type"))
        assertEquals(base64, json.getString("data"))

        val decoded = Base64Compat.decode(json.getString("data"))
        assertEquals("SAMPLE_VOICE_NOTE_PAYLOAD", String(decoded, Charsets.UTF_8))
    }

    @Test
    fun testVoiceNoteTempFileCreation() {
        val tempDir = File(System.getProperty("java.io.tmpdir", "/tmp"))
        val testFile = File(tempDir, "test_voice_note_${System.currentTimeMillis()}.m4a")

        val dummyData = byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04)
        testFile.writeBytes(dummyData)

        assertTrue(testFile.exists())
        assertEquals(5, testFile.length())
        testFile.delete()
    }
}
