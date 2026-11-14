package tech.isano.language.core.engine.jami

import android.content.Context
import android.view.SurfaceView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.jami.jamiclient.AccountManager
import net.jami.jamiclient.ConversationManager
import net.jami.jamiclient.Jami
import net.jami.jamiclient.VideoManager
import org.json.JSONObject

/**
 * Concrete implementation of [JamiDaemon] binding to GNU Jami Android client AAR.
 *
 * Full Video + Audio Pipeline & P2P Swarm File Sharing:
 * Video transmission is enabled by default for face-to-face language interaction to
 * eliminate speaking anxiety. SurfaceViews are bound directly to Jami's video pipeline.
 * Transmits shared flashcard images directly over Jami Swarm data channels.
 */
class JamiDaemonImpl(
    context: Context? = null,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : JamiDaemon {

    private var appContext: Context? = context?.applicationContext
    private var currentAccountId: String? = null
    private var currentSwarmId: String? = null
    private var isListenersAttached: Boolean = false

    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    override val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val _incomingCanvasStrokes = MutableSharedFlow<String>(replay = 50, extraBufferCapacity = 64)
    override val incomingCanvasStrokes: SharedFlow<String> = _incomingCanvasStrokes.asSharedFlow()

    private val _incomingFiles = MutableSharedFlow<String>(replay = 5, extraBufferCapacity = 32)
    override val incomingFiles: SharedFlow<String> = _incomingFiles.asSharedFlow()

    init {
        context?.let { initDaemon(it) }
    }

    /**
     * Initializes the Jami platform runtime using the Android Context.
     */
    override fun initDaemon(context: Context) {
        appContext = context.applicationContext
        Jami.init(context.applicationContext)
        setupEventListeners()
    }

    /**
     * Uses the Jami AccountManager API to generate a standard SIP/DHT OpenDHT account identity.
     * Enables video transmission by default for face-to-face pedagogical interaction.
     */
    override fun createAccount(): JamiAccount {
        val accountInfo = AccountManager.addAccount(
            mapOf(
                "Account.type" to "RING",
                "Account.archiveHasPassword" to "false",
                "Account.videoEnabled" to "true",
                "Account.autoAnswer" to "true"
            )
        )
        currentAccountId = accountInfo.accountId

        return JamiAccount(
            accountId = accountInfo.accountId,
            archiveBytes = accountInfo.archiveData,
            dhtFingerprint = accountInfo.dhtFingerprint
        )
    }

    /**
     * Uses the Jami Swarm / Conversation API to rendezvous in a decentralized language room.
     * Configures the call/swarm with video transmission enabled by default.
     */
    override fun joinLanguageSwarm(nativeLang: String, targetLang: String) {
        val swarmId = generateSwarmId(nativeLang, targetLang)
        currentSwarmId = swarmId
        _callState.value = CallState.Connecting

        setupEventListeners()

        val accountId = currentAccountId ?: run {
            val autoAccount = createAccount()
            autoAccount.accountId
        }

        // Join the decentralized conversation swarm over OpenDHT
        ConversationManager.joinConversation(accountId, swarmId)
    }

    /**
     * Binds local front-facing camera preview and remote peer video streams
     * directly to Android [SurfaceView] instances via Jami's [VideoManager].
     */
    override fun bindVideoViews(localView: SurfaceView, remoteView: SurfaceView) {
        // Paint local camera capture onto foreground PIP view
        VideoManager.setPreviewTarget(localView)

        // Paint remote incoming WebRTC stream onto background view
        val swarmId = currentSwarmId ?: "active_call"
        VideoManager.setRemoteTarget(swarmId, remoteView)
    }

    /**
     * Pushes a local image/file natively to the other user's screen over the Jami Swarm.
     */
    override fun sendFileToSwarm(filePath: String) {
        val swarmId = currentSwarmId ?: return
        coroutineScope.launch {
            val payload = JSONObject().apply {
                put("type", "IMAGE_DROP")
                put("path", filePath)
                put("timestamp", System.currentTimeMillis())
            }.toString()
            ConversationManager.sendMessage(swarmId, payload)
        }
    }

    /**
     * Test / callback bridge method invoked when a remote file transfer completes.
     */
    fun onNativeFileReceived(filePath: String) {
        coroutineScope.launch {
            _incomingFiles.emit(filePath)
        }
    }

    /**
     * Leaves the active Jami Swarm and resets call connectivity state.
     */
    override fun leaveSwarm() {
        val swarmId = currentSwarmId ?: return
        val accountId = currentAccountId ?: ""

        ConversationManager.leaveConversation(accountId, swarmId)
        currentSwarmId = null
        _callState.value = CallState.Idle
    }

    /**
     * Broadcasts a Write-on-Screen Canvas stroke across Jami's active WebRTC DataChannel.
     */
    override fun sendDataChannelPayload(payload: String) {
        val swarmId = currentSwarmId ?: return
        ConversationManager.sendMessage(swarmId, payload)
    }

    /**
     * Subscribes to Jami Call and Message event callbacks, routing real-time
     * network occurrences into [callState], [incomingCanvasStrokes], and [incomingFiles] flows.
     */
    private fun setupEventListeners() {
        if (isListenersAttached) return
        isListenersAttached = true

        ConversationManager.registerCallCallback { conversationId, state, peerCount ->
            coroutineScope.launch {
                if (conversationId == currentSwarmId) {
                    when (state) {
                        "CONNECTED" -> {
                            _callState.value = CallState.ActiveSwarm(
                                swarmId = conversationId,
                                peerCount = peerCount
                            )
                        }
                        "DISCONNECTED" -> {
                            _callState.value = CallState.Idle
                        }
                        "CONNECTING" -> {
                            _callState.value = CallState.Connecting
                        }
                        else -> {
                            _callState.value = CallState.ActiveSwarm(
                                swarmId = conversationId,
                                peerCount = peerCount
                            )
                        }
                    }
                }
            }
        }

        ConversationManager.registerMessageCallback { conversationId, _, payload ->
            coroutineScope.launch {
                if (conversationId == currentSwarmId) {
                    if (payload.contains("\"type\":\"IMAGE_DROP\"")) {
                        try {
                            val json = JSONObject(payload)
                            val path = json.optString("path")
                            if (path.isNotEmpty()) {
                                _incomingFiles.emit(path)
                            }
                        } catch (_: Exception) {}
                    } else {
                        _incomingCanvasStrokes.emit(payload)
                    }
                }
            }
        }
    }

    companion object {
        private val KNOWN_LANGUAGES = mapOf(
            "english" to "en",
            "french" to "fr",
            "kinyarwanda" to "rw",
            "spanish" to "es",
            "swahili" to "sw",
            "portuguese" to "pt",
            "arabic" to "ar",
            "german" to "de",
            "japanese" to "ja"
        )

        /**
         * Predictably hashes complementary language pairs to an identical Swarm ID.
         * Example: ("English", "Kinyarwanda") -> "swarm_en_rw"
         *          ("Kinyarwanda", "English") -> "swarm_en_rw"
         */
        fun generateSwarmId(nativeLang: String, targetLang: String): String {
            val code1 = normalizeToCode(nativeLang)
            val code2 = normalizeToCode(targetLang)
            val sortedCodes = listOf(code1, code2).sorted()
            return "swarm_${sortedCodes[0]}_${sortedCodes[1]}"
        }

        private fun normalizeToCode(lang: String): String {
            val cleaned = lang.trim().lowercase()
            return KNOWN_LANGUAGES[cleaned] ?: cleaned.take(2)
        }
    }
}
