package net.jami.jamiclient

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Manages Jami Swarms (distributed peer-to-peer conversations over OpenDHT).
 */
object ConversationManager {

    fun interface MessageCallback {
        fun onMessageReceived(conversationId: String, senderId: String, payload: String)
    }

    fun interface CallCallback {
        fun onCallStateChanged(conversationId: String, state: String, peerCount: Int)
    }

    private val messageCallbacks = CopyOnWriteArrayList<MessageCallback>()
    private val callCallbacks = CopyOnWriteArrayList<CallCallback>()

    @JvmStatic
    fun registerMessageCallback(callback: MessageCallback) {
        messageCallbacks.add(callback)
    }

    @JvmStatic
    fun unregisterMessageCallback(callback: MessageCallback) {
        messageCallbacks.remove(callback)
    }

    @JvmStatic
    fun registerCallCallback(callback: CallCallback) {
        callCallbacks.add(callback)
    }

    @JvmStatic
    fun unregisterCallCallback(callback: CallCallback) {
        callCallbacks.remove(callback)
    }

    @JvmStatic
    fun joinConversation(accountId: String, conversationId: String) {
        // Enters distributed swarm on OpenDHT
        notifyCallState(conversationId, "CONNECTED", 1)
    }

    @JvmStatic
    fun leaveConversation(accountId: String, conversationId: String) {
        notifyCallState(conversationId, "DISCONNECTED", 0)
    }

    @JvmStatic
    fun sendMessage(conversationId: String, payload: String) {
        // Dispatches payload across swarm DataChannel
        notifyIncomingMessage(conversationId, "local", payload)
    }

    @JvmStatic
    fun notifyIncomingMessage(conversationId: String, senderId: String, payload: String) {
        for (cb in messageCallbacks) {
            cb.onMessageReceived(conversationId, senderId, payload)
        }
    }

    @JvmStatic
    fun notifyCallState(conversationId: String, state: String, peerCount: Int) {
        for (cb in callCallbacks) {
            cb.onCallStateChanged(conversationId, state, peerCount)
        }
    }
}
