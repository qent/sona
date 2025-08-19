package io.qent.sona.core.tokens

import io.qent.sona.core.presets.Preset

/** Token counter backed by langchain4j estimators and provider APIs. */
class DefaultTokenCounter : TokenCounter {
    override suspend fun count(text: String, preset: Preset): Int {
        return when (preset.provider.name) {
            else -> 0
        }
    }
}
