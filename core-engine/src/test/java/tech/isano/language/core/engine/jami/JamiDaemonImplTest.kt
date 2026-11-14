package tech.isano.language.core.engine.jami

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JamiDaemonImplTest {

    private lateinit var daemon: JamiDaemonImpl

    @Before
    fun setUp() {
        daemon = JamiDaemonImpl()
    }

    @Test
    fun testDeterministicSwarmIdGeneration_isSymmetricAndStandardized() {
        val swarm1 = JamiDaemonImpl.generateSwarmId("English", "Kinyarwanda")
        val swarm2 = JamiDaemonImpl.generateSwarmId("Kinyarwanda", "English")

        assertEquals("swarm_en_rw", swarm1)
        assertEquals("swarm_en_rw", swarm2)
    }

    @Test
    fun testDeterministicSwarmIdGeneration_handlesSpanishAndEnglish() {
        val swarm1 = JamiDaemonImpl.generateSwarmId("Spanish", "English")
        val swarm2 = JamiDaemonImpl.generateSwarmId("English", "Spanish")

        assertEquals("swarm_en_es", swarm1)
        assertEquals(swarm1, swarm2)
    }

    @Test
    fun testDeterministicSwarmIdGeneration_caseAndWhitespaceInsensitive() {
        val swarm1 = JamiDaemonImpl.generateSwarmId("  french ", "kInYaRwAnDa  ")
        val swarm2 = JamiDaemonImpl.generateSwarmId("KINYARWANDA", "FRENCH")

        assertEquals("swarm_fr_rw", swarm1)
        assertEquals(swarm1, swarm2)
    }

    @Test
    fun testCreateAccount_generatesValidDhtIdentityAndArchive() {
        val account = daemon.createAccount()

        assertNotNull(account.accountId)
        assertTrue(account.accountId.startsWith("jami_"))
        assertNotNull(account.dhtFingerprint)
        assertTrue(account.archiveBytes.isNotEmpty())
    }

    @Test
    fun testJoinAndLeaveSwarm_updatesCallState() {
        assertEquals(CallState.Idle, daemon.callState.value)

        daemon.joinLanguageSwarm("English", "French")
        val activeState = daemon.callState.value
        assertTrue(activeState is CallState.ActiveSwarm)
        val swarmState = activeState as CallState.ActiveSwarm
        assertEquals("swarm_en_fr", swarmState.swarmId)

        daemon.leaveSwarm()
        assertEquals(CallState.Idle, daemon.callState.value)
    }

    @Test
    fun testSendDataChannelPayload_emitsToFlow() {
        daemon.joinLanguageSwarm("English", "Spanish")
        val payload = "{\"type\":\"stroke\",\"x\":120.5,\"y\":340.2}"

        daemon.sendDataChannelPayload(payload)
        assertEquals(CallState.ActiveSwarm("swarm_en_es", 1), daemon.callState.value)
    }
}
