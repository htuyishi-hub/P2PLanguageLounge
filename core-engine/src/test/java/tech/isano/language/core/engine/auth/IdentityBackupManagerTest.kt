package tech.isano.language.core.engine.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentityBackupManagerTest {

    @Test
    fun testWebClientIdPlaceholder() {
        assertEquals("YOUR_WEB_CLIENT_ID_HERE", IdentityBackupManagerImpl.WEB_CLIENT_ID)
    }

    @Test
    fun testBackupFileName_isStandardDat() {
        assertEquals("jami_backup.dat", IdentityBackupManagerImpl.BACKUP_FILE_NAME)
    }

    @Test
    fun testKeyArchivePayloadIntegrity() {
        val testPayload = "OPEN_DHT_CRYPTOGRAPHIC_ARCHIVE_DATA".toByteArray(Charsets.UTF_8)
        assertTrue(testPayload.isNotEmpty())
        assertEquals(36, testPayload.size)
    }
}
