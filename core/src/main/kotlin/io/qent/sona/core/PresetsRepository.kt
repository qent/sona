package io.qent.sona.core

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
            LlmModel("claude-sonnet-4-20250514", maxContextTokens = 200_000),
            LlmModel("claude-3-7-sonnet-20250219", maxContextTokens = 200_000),
            LlmModel("claude-3-5-haiku-20241022", maxContextTokens = 200_000),
        ),
    ),
    OpenAI(
        "https://api.openai.com/v1/",
        listOf(
            LlmModel("o3", maxContextTokens = 128_000),
            LlmModel("gpt-4.1", maxContextTokens = 128_000),
            LlmModel("gpt-4.1-mini", maxContextTokens = 128_000),
            LlmModel("gpt-4o", maxContextTokens = 128_000),
        ),
    ),
    Deepseek(
        "https://api.deepseek.com/v1/",
        listOf(
            LlmModel("deepseek-chat", maxContextTokens = 128_000),
            LlmModel("deepseek-reasoner", maxContextTokens = 128_000),
        ),
    ),
    Gemini(
        "https://generativelanguage.googleapis.com/v1beta/",
        listOf(
            LlmModel("gemini-2.5-pro", maxContextTokens = 128_000),
            LlmModel("gemini-2.5-flash", maxContextTokens = 128_000),
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

