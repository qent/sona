package io.qent.sona.core

enum class LlmProvider(val defaultEndpoint: String, val models: List<String>) {
    Anthropic(
        "https://api.anthropic.com/v1/",
        listOf("sonet 3.7", "sonet 4.0", "haiku 3.5"),
    ),
    OpenAI(
        "https://api.openai.com/v1/",
        listOf("o3", "4.1"),
    ),
    Deepseek(
        "https://api.deepseek.com/v1/",
        listOf("v3"),
    ),
    Gemini(
        "https://generativelanguage.googleapis.com/v1beta/",
        listOf("2.5"),
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

