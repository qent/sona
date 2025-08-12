package io.qent.sona.core.prompts

interface UserPromptRepository {
    suspend fun load(): String
    suspend fun save(prompt: String)
}
