package tech.isano.language.core.engine.auth

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.isano.language.core.engine.jami.JamiDaemonImpl

class CloudIdentityManagerTest {

    private lateinit var repository: InMemoryCloudAuthRepository
    private lateinit var manager: CloudIdentityManager
    private val testDaemon = JamiDaemonImpl()

    @Before
    fun setUp() {
        repository = InMemoryCloudAuthRepository()
        manager = CloudIdentityManager(repository, testDaemon)
    }

    @Test
    fun testBackupAndRestore_withValidPin_restoresIdenticalArchive() = runBlocking {
        val originalArchive = "SOVEREIGN_OPEN_DHT_IDENTITY_ARCHIVE_DATA_123456789".toByteArray(Charsets.UTF_8)
        val username = "kimenyi"
        val pin = "4812"

        val backupSuccess = manager.backupIdentity(username, pin, originalArchive)
        assertTrue(backupSuccess)

        val restoredArchive = manager.restoreIdentity(username, pin)
        assertNotNull(restoredArchive)
        assertArrayEquals(originalArchive, restoredArchive)
    }

    @Test
    fun testRestore_withWrongPin_returnsNull() = runBlocking {
        val originalArchive = "ENCRYPTED_SECRET_JAMI_ARCHIVE_BYTES".toByteArray(Charsets.UTF_8)
        val username = "mugisha"
        val correctPin = "9922"
        val wrongPin = "1133"

        manager.backupIdentity(username, correctPin, originalArchive)

        val restoredArchive = manager.restoreIdentity(username, wrongPin)
        assertNull(restoredArchive)
    }

    @Test
    fun testRestore_nonExistentUser_returnsNull() = runBlocking {
        val restoredArchive = manager.restoreIdentity("ghost_user", "1234")
        assertNull(restoredArchive)
    }

    @Test
    fun testRegisterAndBackup_createsAndStoresAccount() = runBlocking {
        val username = "isano_user"
        val pin = "7741"

        val account = manager.registerAndBackup(username, pin)
        assertNotNull(account.accountId)
        assertTrue(account.archiveBytes.isNotEmpty())

        val restoredArchive = manager.restoreIdentity(username, pin)
        assertNotNull(restoredArchive)
        assertArrayEquals(account.archiveBytes, restoredArchive)
    }

    @Test
    fun testUsernameCaseInsensitivityAndTrimming() = runBlocking {
        val originalArchive = "CASE_INSENSITIVE_TEST".toByteArray(Charsets.UTF_8)
        val pin = "5566"

        manager.backupIdentity("  KiMeNyI  ", pin, originalArchive)

        val restoredArchive = manager.restoreIdentity("kimenyi", pin)
        assertNotNull(restoredArchive)
        assertArrayEquals(originalArchive, restoredArchive)
    }

    @Test
    fun testLockboxAuth_newUsername_createsAndLocksVaultSuccessfully() = runBlocking {
        val (success, error) = manager.lockboxAuth("new_sovereign_user", "2026")
        assertTrue(success)
        assertNull(error)

        // Verifying it can now be unlocked
        val (unlockSuccess, unlockError) = manager.lockboxAuth("new_sovereign_user", "2026")
        assertTrue(unlockSuccess)
        assertNull(unlockError)
    }

    @Test
    fun testLockboxAuth_existingUsername_wrongPin_failsWithErrorMessage() = runBlocking {
        manager.lockboxAuth("vault_user", "1234")

        val (failSuccess, failError) = manager.lockboxAuth("vault_user", "9999")
        assertTrue(!failSuccess)
        assertNotNull(failError)
        assertTrue(failError!!.contains("Incorrect PIN"))
    }

    @Test
    fun testChangePin_withValidOldPin_updatesVaultSuccessfully() = runBlocking {
        manager.lockboxAuth("reset_user", "1111")

        // Change PIN from 1111 to 2222
        val (success, error) = manager.changePin("reset_user", "1111", "2222")
        assertTrue(success)
        assertNull(error)

        // Old PIN should now fail
        val (oldPinSuccess, _) = manager.lockboxAuth("reset_user", "1111")
        assertTrue(!oldPinSuccess)

        // New PIN should unlock
        val (newPinSuccess, _) = manager.lockboxAuth("reset_user", "2222")
        assertTrue(newPinSuccess)
    }

    @Test
    fun testChangePin_withWrongOldPin_failsAndPreservesOldPin() = runBlocking {
        manager.lockboxAuth("reset_user2", "3333")

        // Attempt change with wrong old PIN
        val (success, error) = manager.changePin("reset_user2", "9999", "4444")
        assertTrue(!success)
        assertNotNull(error)

        // Original PIN should still work
        val (origSuccess, _) = manager.lockboxAuth("reset_user2", "3333")
        assertTrue(origSuccess)
    }
}
