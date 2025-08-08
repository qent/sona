package io.qent.sona.core.model

import dev.langchain4j.model.anthropic.AnthropicTokenUsage
import dev.langchain4j.model.openai.OpenAiTokenUsage
import org.junit.Assert.assertEquals
import org.junit.Test

class TokenUsageInfoTest {

    @Test
    fun `plus combines all fields`() {
        val a = TokenUsageInfo(1, 2, 3, 4)
        val b = TokenUsageInfo(5, 6, 7, 8)
        assertEquals(TokenUsageInfo(6, 8, 10, 12), a + b)
    }

    @Test
    fun `cost is calculated from model pricing`() {
        val info = TokenUsageInfo(
            outputTokens = 1000,
            inputTokens = 2000,
            cacheCreationInputTokens = 4000,
            cacheReadInputTokens = 8000
        )
        val model = io.qent.sona.core.presets.LlmModel(
            name = "test",
            outputCostPerMTokens = 20.0,
            inputCostPerMTokens = 10.0,
            cacheCreationCostPerMTokens = 5.0,
            cacheReadCostPerMTokens = 2.5
        )
        val expected = 0.08
        assertEquals(expected, info.cost(model), 1e-9)
    }

    @Test
    fun `toInfo converts OpenAI usage including cached tokens`() {
        val inputDetails = OpenAiTokenUsage.InputTokensDetails.builder()
            .cachedTokens(3)
            .build()
        val usage = OpenAiTokenUsage.builder()
            .inputTokenCount(10)
            .outputTokenCount(20)
            .inputTokensDetails(inputDetails)
            .build()
        val info = usage.toInfo()
        assertEquals(TokenUsageInfo(20, 10, 0, 3), info)
    }

    @Test
    fun `toInfo converts Anthropic usage with cache stats`() {
        val usage = AnthropicTokenUsage.builder()
            .inputTokenCount(5)
            .outputTokenCount(7)
            .cacheCreationInputTokens(2)
            .cacheReadInputTokens(1)
            .build()
        val info = usage.toInfo()
        assertEquals(TokenUsageInfo(7, 5, 2, 1), info)
    }
}

