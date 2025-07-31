package com.github.d0lfin.sona.logic

interface ChatSettingsRepository {
    suspend fun load(): ChatSettings
    suspend fun save(settings: ChatSettings)
}
