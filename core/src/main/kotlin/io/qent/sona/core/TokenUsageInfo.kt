package io.qent.sona.core

import dev.langchain4j.model.anthropic.AnthropicTokenUsage
import dev.langchain4j.model.output.TokenUsage

/**
 * Container for token usage statistics including cached tokens.
 *
 * [outputTokens] and [inputTokens] represent tokens generated and
 * consumed for a response respectively. [cachedOutputTokens] and
 * [cachedInputTokens] track tokens served from or stored in provider
 * caches. Providers that do not expose cache statistics should set
 * cached counts to zero.
 */
data class TokenUsageInfo(
    val outputTokens: Int = 0,
    val inputTokens: Int = 0,
    val cachedOutputTokens: Int = 0,
    val cachedInputTokens: Int = 0,
) {
    operator fun plus(other: TokenUsageInfo) = TokenUsageInfo(
        outputTokens + other.outputTokens,
        inputTokens + other.inputTokens,
        cachedOutputTokens + other.cachedOutputTokens,
        cachedInputTokens + other.cachedInputTokens,
    )
}

/** Convert LangChain4j [TokenUsage] to [TokenUsageInfo]. */
fun TokenUsage.toInfo(): TokenUsageInfo {
    val cachedInput =
        if (this is AnthropicTokenUsage) {
            (cacheCreationInputTokens() ?: 0) + (cacheReadInputTokens() ?: 0)
        } else {
            0
        }

    return TokenUsageInfo(
        outputTokens = outputTokenCount(),
        inputTokens = inputTokenCount(),
        cachedOutputTokens = 0,
        cachedInputTokens = cachedInput,
    )
}

