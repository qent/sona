package io.qent.sona.core.tokens

import dev.langchain4j.data.message.ChatMessage
import io.qent.sona.core.presets.Preset

/** Counts tokens in messages for a given model preset. */
interface TokenCounter {
    /** Return number of tokens contained in [message] for [preset]. */
    suspend fun count(message: ChatMessage, preset: Preset): Int
}
