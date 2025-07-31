package io.qent.sona.core.model

import dev.langchain4j.service.MemoryId
import dev.langchain4j.service.TokenStream
import dev.langchain4j.service.UserMessage

interface SonaAiService {
    fun chat(
        @MemoryId chatId: String,
        @UserMessage message: String
    ): TokenStream
}
