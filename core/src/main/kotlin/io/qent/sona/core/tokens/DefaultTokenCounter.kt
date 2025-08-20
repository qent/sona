package io.qent.sona.core.tokens

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.models.messages.MessageCountTokensParams
import dev.langchain4j.model.googleai.GoogleAiGeminiTokenCountEstimator
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.ToolExecutionResultMessage
import dev.langchain4j.data.message.UserMessage
import io.qent.sona.core.presets.Preset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Token counter backed by langchain4j estimators and provider APIs. */
class DefaultTokenCounter : TokenCounter {
    override suspend fun count(message: ChatMessage, preset: Preset): Int =
        when (preset.provider.name) {
            "Anthropic" -> countAnthropic(message, preset)
            "Gemini" -> countGemini(message, preset)
            "OpenAI" -> countOpenAi(message, preset)
            else -> 0
        }

    private suspend fun countAnthropic(message: ChatMessage, preset: Preset): Int =
        withContext(Dispatchers.IO) {
            val baseUrl = preset.apiEndpoint.substringBefore("/v1")
            val client = AnthropicOkHttpClient.builder()
                .apiKey(preset.apiKey)
                .baseUrl(baseUrl)
                .build()
            try {
                val builder = MessageCountTokensParams.builder().model(preset.model)
                when (message) {
                    is SystemMessage -> builder.system(message.text())
                    is AiMessage -> builder.addAssistantMessage(message.text())
                    is UserMessage -> builder.addUserMessage(message.singleText())
                    is ToolExecutionResultMessage -> builder.addUserMessage(message.text())
                    else -> builder.addUserMessage(message.toString())
                }
                client.messages().countTokens(builder.build()).inputTokens().toInt()
            } catch (e: Exception) {
                0
            } finally {
                client.close()
            }
        }

    private suspend fun countGemini(message: ChatMessage, preset: Preset): Int =
        withContext(Dispatchers.IO) {
            try {
                GoogleAiGeminiTokenCountEstimator.builder()
                    .modelName(preset.model)
                    .apiKey(preset.apiKey)
                    .baseUrl(preset.apiEndpoint)
                    .build()
                    .estimateTokenCountInMessage(message)
            } catch (e: Exception) {
                0
            }
        }

    private fun countOpenAi(message: ChatMessage, preset: Preset): Int =
        try {
            OpenAiTokenCountEstimator(preset.model).estimateTokenCountInMessage(message)
        } catch (e: Exception) {
            0
        }
}
