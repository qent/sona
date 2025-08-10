package io.qent.sona.core.chat

/**
 * Represents chat side effects that operate on [ChatStateFlow].
 */
interface ChatSession {
    suspend fun send(text: String)
    fun stop()
    suspend fun deleteFrom(idx: Int)
    fun toggleAutoApproveTools()
}

