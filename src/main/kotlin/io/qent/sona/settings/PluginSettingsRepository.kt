package io.qent.sona.settings

import io.qent.sona.core.Settings
import io.qent.sona.core.SettingsRepository
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.PersistentStateComponent

@Service
@State(name = "PluginSettings", storages = [Storage("plugin_settings.xml")])
class PluginSettingsRepository :
    SettingsRepository,
    PersistentStateComponent<PluginSettingsRepository.PluginSettingsState>
{
    data class PluginSettingsState(
        var apiKey: String = "",
        var apiEndpoint: String = "https://api.anthropic.com/v1/",
        var model: String = "claude-3-5-haiku-20241022"
    )

    private var pluginSettingsState = PluginSettingsState()

    override fun getState() = pluginSettingsState

    override fun loadState(state: PluginSettingsState) {
        this.pluginSettingsState = state
    }

    override suspend fun load() = Settings(
        pluginSettingsState.apiKey,
        pluginSettingsState.apiEndpoint,
        pluginSettingsState.model
    )
}
