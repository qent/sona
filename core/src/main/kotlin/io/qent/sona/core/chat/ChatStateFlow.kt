package io.qent.sona.core.chat

import io.qent.sona.core.model.TokenUsageInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

class ChatStateFlow(
    private val chatRepository: ChatRepository,
): Flow<Chat> {

    private val mutableSharedState = MutableSharedFlow<Chat>(
        replay = 1,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.SUSPEND
    )

    var currentState = Chat("", TokenUsageInfo())
        private set

    suspend fun loadChat(chatId: String) {
        val messages = chatRepository.loadMessages(chatId)
        val usage = chatRepository.loadTokenUsage(chatId)
        emit(Chat(chatId, usage, messages))
    }

    fun emit(chat: Chat) {
        currentState = chat
        mutableSharedState.tryEmit(chat)
    }

    override suspend fun collect(collector: FlowCollector<Chat>) {
        mutableSharedState.collect(collector)
    }
}
