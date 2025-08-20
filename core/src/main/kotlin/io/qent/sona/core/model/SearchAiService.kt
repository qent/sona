package io.qent.sona.core.model

import dev.langchain4j.model.chat.response.ChatResponse
import dev.langchain4j.service.UserMessage

interface SearchAiService {
    fun chat(
        @UserMessage message: String
    ): ChatResponse
}
