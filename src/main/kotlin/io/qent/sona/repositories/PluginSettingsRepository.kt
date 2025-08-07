package io.qent.sona.repositories

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import io.qent.sona.core.settings.Settings
import io.qent.sona.core.settings.SettingsRepository

@Service
@State(name = "PluginSettings", storages = [Storage("plugin_settings.xml")])
class PluginSettingsRepository :
    SettingsRepository,
    PersistentStateComponent<PluginSettingsRepository.PluginSettingsState> {
    data class PluginSettingsState(
        var ignoreHttpsErrors: Boolean = false,
        var cacheSystemPrompts: Boolean = false,
        var cacheToolDescriptions: Boolean = false,
    )

    private var pluginSettingsState = PluginSettingsState()

    override fun getState() = pluginSettingsState

    override fun loadState(state: PluginSettingsState) {
        this.pluginSettingsState = state
    }

    override suspend fun load() = Settings(
        pluginSettingsState.ignoreHttpsErrors,
        pluginSettingsState.cacheSystemPrompts,
        pluginSettingsState.cacheToolDescriptions,
    )
}
