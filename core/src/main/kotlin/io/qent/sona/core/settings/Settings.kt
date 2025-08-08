package io.qent.sona.core.settings

data class Settings(
    val ignoreHttpsErrors: Boolean,
    val cacheSystemPrompts: Boolean,
    val cacheToolDescriptions: Boolean,
    val apiRetries: Int,
)
