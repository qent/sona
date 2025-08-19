package io.qent.sona.core.model

import dev.langchain4j.service.TokenStream
import dev.langchain4j.service.UserMessage

interface SearchAiService {
    fun chat(
        @UserMessage message: String
    ): TokenStream
}
