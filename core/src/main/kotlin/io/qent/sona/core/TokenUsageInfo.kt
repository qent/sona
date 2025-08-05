package io.qent.sona.core

import dev.langchain4j.model.anthropic.AnthropicTokenUsage
import dev.langchain4j.model.openai.OpenAiTokenUsage
import dev.langchain4j.model.output.TokenUsage

/**
 * Container for token usage statistics including cached tokens.
 *
 * [outputTokens] and [inputTokens] represent tokens generated and
 * consumed for a response respectively. [cacheCreationInputTokens] and
 * [cacheReadInputTokens] track tokens served from or stored in provider
 * caches. Providers that do not expose cache statistics should set
 * cached counts to zero.
 */
data class TokenUsageInfo(
    val outputTokens: Int = 0,
    val inputTokens: Int = 0,
    val cacheCreationInputTokens: Int = 0,
    val cacheReadInputTokens: Int = 0,
) {
    operator fun plus(other: TokenUsageInfo) = TokenUsageInfo(
        outputTokens + other.outputTokens,
        inputTokens + other.inputTokens,
        cacheCreationInputTokens + other.cacheCreationInputTokens,
        cacheReadInputTokens + other.cacheReadInputTokens,
    )
}

/** Convert LangChain4j [TokenUsage] to [TokenUsageInfo]. */
fun TokenUsage.toInfo(): TokenUsageInfo {
    val cacheCreationInputTokens = when (this) {
        is AnthropicTokenUsage -> cacheCreationInputTokens()
        else -> 0
    }
    val cacheReadInputTokens = when (this) {
        is AnthropicTokenUsage -> cacheReadInputTokens()
        is OpenAiTokenUsage -> inputTokensDetails().cachedTokens()
        else -> 0
    }

    return TokenUsageInfo(
        outputTokens = outputTokenCount(),
        inputTokens = inputTokenCount(),
        cacheCreationInputTokens = cacheCreationInputTokens,
        cacheReadInputTokens = cacheReadInputTokens,
    )
}

