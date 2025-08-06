package io.qent.sona.core.presets

/** Information about a specific LLM model. */
data class LlmModel(
    val name: String,
    val outputCostPerMTokens: Double = 0.0,
    val inputCostPerMTokens: Double = 0.0,
    val cacheCreationCostPerMTokens: Double = 0.0,
    val cacheReadCostPerMTokens: Double = 0.0,
    val maxContextTokens: Int = 0,
)

enum class LlmProvider(val defaultEndpoint: String, val models: List<LlmModel>) {
    Anthropic(
        "https://api.anthropic.com/v1/",
        listOf(
            // Pricing: https://www.anthropic.com/pricing#api
            LlmModel(
                name = "claude-sonnet-4-20250514",
                outputCostPerMTokens = 15.0,
                inputCostPerMTokens = 3.0,
                cacheCreationCostPerMTokens = 3.75,
                cacheReadCostPerMTokens = 0.30,
                maxContextTokens = 200_000,
            ),
            LlmModel(
                name = "claude-3-7-sonnet-20250219",
                outputCostPerMTokens = 15.0,
                inputCostPerMTokens = 3.0,
                cacheCreationCostPerMTokens = 3.75,
                cacheReadCostPerMTokens = 0.30,
                maxContextTokens = 200_000,
            ),
            LlmModel(
                name = "claude-3-5-haiku-20241022",
                outputCostPerMTokens = 4.0,
                inputCostPerMTokens = 0.80,
                cacheCreationCostPerMTokens = 1.0,
                cacheReadCostPerMTokens = 0.08,
                maxContextTokens = 200_000,
            ),
        ),
    ),
    OpenAI(
        "https://api.openai.com/v1/",
        listOf(
            // Pricing: https://platform.openai.com/docs/pricing
            LlmModel(
                name = "o3",
                outputCostPerMTokens = 8.0,
                inputCostPerMTokens = 2.0,
                cacheReadCostPerMTokens = 0.5,
                maxContextTokens = 128_000,
            ),
            LlmModel(
                name = "gpt-4.1",
                outputCostPerMTokens = 8.0,
                inputCostPerMTokens = 2.0,
                cacheReadCostPerMTokens = 0.5,
                maxContextTokens = 128_000,
            ),
            LlmModel(
                name = "gpt-4.1-mini",
                outputCostPerMTokens = 1.6,
                inputCostPerMTokens = 0.4,
                cacheReadCostPerMTokens = 0.1,
                maxContextTokens = 128_000,
            ),
            LlmModel(
                name = "gpt-4o",
                outputCostPerMTokens = 10.0,
                inputCostPerMTokens = 2.5,
                cacheReadCostPerMTokens = 1.25,
                maxContextTokens = 128_000,
            ),
        ),
    ),
    Deepseek(
        "https://api.deepseek.com/v1/",
        listOf(
            // Pricing: https://api-docs.deepseek.com/quick_start/pricing
            LlmModel(
                name = "deepseek-chat",
                outputCostPerMTokens = 1.10,
                inputCostPerMTokens = 0.27,
                cacheReadCostPerMTokens = 0.07,
                maxContextTokens = 64_000,
            ),
            LlmModel(
                name = "deepseek-reasoner",
                outputCostPerMTokens = 2.19,
                inputCostPerMTokens = 0.55,
                cacheReadCostPerMTokens = 0.14,
                maxContextTokens = 64_000,
            ),
        ),
    ),
    Gemini(
        "https://generativelanguage.googleapis.com/v1beta/",
        listOf(
            // Pricing: https://ai.google.dev/gemini-api/docs/pricing
            LlmModel(
                name = "gemini-2.5-pro",
                outputCostPerMTokens = 10.0,
                inputCostPerMTokens = 1.25,
                cacheCreationCostPerMTokens = 0.31,
                maxContextTokens = 128_000,
            ),
            LlmModel(
                name = "gemini-2.5-flash",
                outputCostPerMTokens = 2.5,
                inputCostPerMTokens = 0.30,
                cacheCreationCostPerMTokens = 0.075,
                maxContextTokens = 128_000,
            ),
        ),
    );
}

data class Preset(
    val name: String,
    val provider: LlmProvider,
    val apiEndpoint: String,
    val model: String,
    val apiKey: String,
)

data class Presets(
    val active: Int,
    val presets: List<Preset>,
)

interface PresetsRepository {
    suspend fun load(): Presets
    suspend fun save(presets: Presets)
}

