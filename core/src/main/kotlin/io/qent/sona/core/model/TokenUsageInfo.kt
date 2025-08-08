package io.qent.sona.core.model

import dev.langchain4j.model.anthropic.AnthropicTokenUsage
import dev.langchain4j.model.openai.OpenAiTokenUsage
import dev.langchain4j.model.output.TokenUsage
import io.qent.sona.core.presets.LlmModel

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
        is OpenAiTokenUsage -> inputTokensDetails()?.cachedTokens() ?: 0
        else -> 0
    }

    return TokenUsageInfo(
        outputTokens = outputTokenCount(),
        inputTokens = inputTokenCount(),
        cacheCreationInputTokens = cacheCreationInputTokens,
        cacheReadInputTokens = cacheReadInputTokens,
    )
}

/**
 * Calculate the cost in USD for this token usage given the model pricing.
 */
fun TokenUsageInfo.cost(model: LlmModel?): Double {
    if (model == null) return 0.0
    fun calc(tokens: Int, price: Double) = tokens / 1_000_000.0 * price
    return calc(outputTokens, model.outputCostPerMTokens) +
        calc(inputTokens, model.inputCostPerMTokens) +
        calc(cacheCreationInputTokens, model.cacheCreationCostPerMTokens) +
        calc(cacheReadInputTokens, model.cacheReadCostPerMTokens)
}

