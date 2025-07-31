package io.qent.sona.core.settings

interface SettingsRepository {
    suspend fun load(): Settings
}
