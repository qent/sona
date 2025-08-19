package io.qent.sona.core.tokens

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.models.messages.MessageCountTokensParams
import com.anthropic.models.messages.MessageParam
import com.anthropic.models.messages.Model
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator
import dev.langchain4j.model.googleai.GoogleAiGeminiTokenCountEstimator
import io.qent.sona.core.presets.Preset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Token counter backed by langchain4j estimators and provider APIs. */
class DefaultTokenCounter : TokenCounter {
    override suspend fun count(text: String, preset: Preset): Int {
        return when (preset.provider.name) {
            "Anthropic" -> anthropicCount(text, preset)
            "Gemini" -> GoogleAiGeminiTokenCountEstimator.builder().modelName(preset.model).build().estimateTokenCountInText(text)
            else -> OpenAiTokenCountEstimator(preset.model).estimateTokenCountInText(text)
        }
    }

    private suspend fun anthropicCount(text: String, preset: Preset): Int = withContext(Dispatchers.IO) {
        val client = AnthropicOkHttpClient.builder()
            .apiKey(preset.apiKey)
            .baseUrl(preset.apiEndpoint)
            .build()
        try {
            val params = MessageCountTokensParams.builder()
                .model(Model.of(preset.model))
                .addMessage(
                    MessageParam.builder()
                        .role(MessageParam.Role.USER)
                        .content(text)
                        .build()
                )
                .build()
            return@withContext client.messages().countTokens(params).inputTokens().toInt()
        } finally {
            client.close()
        }
    }
}
