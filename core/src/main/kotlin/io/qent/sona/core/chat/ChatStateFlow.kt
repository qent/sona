package io.qent.sona.core.chat

import io.qent.sona.core.model.TokenUsageInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

class ChatStateFlow(
    private val chatRepository: ChatRepository,
    scope: CoroutineScope,
): Flow<Chat> {

    private val scope = scope + Dispatchers.IO
    private val mutableSharedState = MutableSharedFlow<Chat>()

    var currentState = Chat("", TokenUsageInfo())
        private set

    suspend fun loadChat(chatId: String) {
        val messages = chatRepository.loadMessages(chatId)
        val usage = chatRepository.loadTokenUsage(chatId)
        emit(Chat(chatId, usage, messages))
    }

    fun emit(chat: Chat) {
        currentState = chat // BEFORE SCOPE LAUNCH!
        scope.launch {
            mutableSharedState.emit(chat)
        }
    }

    override suspend fun collect(collector: FlowCollector<Chat>) {
        mutableSharedState.collect(collector)
    }
}
