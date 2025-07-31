package io.qent.sona.core

enum class LlmProvider(val defaultEndpoint: String, val models: List<String>) {
    Anthropic(
        "https://api.anthropic.com/v1/",
        listOf("claude-sonnet-4-20250514", "claude-3-7-sonnet-20250219", "claude-3-5-haiku-20241022"),
    ),
    OpenAI(
        "https://api.openai.com/v1/",
        listOf("o3", "gpt-4.1", "gpt-4.1-mini", "gpt-4o"),
    ),
    Deepseek(
        "https://api.deepseek.com/v1/",
        listOf("deepseek-chat", "deepseek-reasoner"),
    ),
    Gemini(
        "https://generativelanguage.googleapis.com/v1beta/",
        listOf("gemini-2.5-pro", "gemini-2.5-flash"),
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

