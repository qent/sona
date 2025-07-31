package io.qent.sona.core

interface SettingsRepository {
    suspend fun load(): Settings
}
