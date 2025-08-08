package io.qent.sona.core.presets

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** Information about a specific LLM model. */
data class LlmModel(
    val name: String,
    val outputCostPerMTokens: Double = 0.0,
    val inputCostPerMTokens: Double = 0.0,
    val cacheCreationCostPerMTokens: Double = 0.0,
    val cacheReadCostPerMTokens: Double = 0.0,
    val maxContextTokens: Int = 0,
)

/** Information about a provider and its models. */
data class LlmProvider(
    val name: String,
    val defaultEndpoint: String,
    val models: List<LlmModel>,
)

/** Loads provider information from a JSON resource for easy updates. */
object LlmProviders {
    private val gson = Gson()
    private val type = object : TypeToken<List<LlmProvider>>() {}.type

    private val customOpenAi = LlmProvider(
        name = "Custom OpenAI",
        defaultEndpoint = "https://api.openai.com/v1/",
        models = emptyList(),
    )

    val entries: List<LlmProvider> by lazy {
        val jsonProviders: List<LlmProvider> =
            LlmProviders::class.java.getResourceAsStream("/providers.json")!!.use { stream ->
                gson.fromJson(stream.reader(), type)
            }
        jsonProviders + customOpenAi
    }

    fun find(name: String): LlmProvider? = entries.find { it.name == name }

    val default: LlmProvider
        get() = entries.first()
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

