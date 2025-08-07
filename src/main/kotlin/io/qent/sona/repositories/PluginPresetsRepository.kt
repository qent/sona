package io.qent.sona.repositories

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import io.qent.sona.core.presets.LlmProvider
import io.qent.sona.core.presets.LlmProviders
import io.qent.sona.core.presets.Preset
import io.qent.sona.core.presets.Presets
import io.qent.sona.core.presets.PresetsRepository

@Service
@State(name = "PluginPresets", storages = [Storage("presets.xml")])
class PluginPresetsRepository : PresetsRepository,
    PersistentStateComponent<PluginPresetsRepository.PluginPresetsState> {

    data class StoredPreset(
        var name: String = "",
        var provider: String = LlmProviders.default.name,
        var apiEndpoint: String = LlmProviders.default.defaultEndpoint,
        var model: String = LlmProviders.default.models.first().name,
        var apiKey: String = "",
    )

    data class PluginPresetsState(
        var active: Int = 0,
        var presets: MutableList<StoredPreset> = mutableListOf(),
    )

    private var state = PluginPresetsState()

    override fun getState(): PluginPresetsState = state

    override fun loadState(state: PluginPresetsState) {
        this.state = state
    }

    override suspend fun load(): Presets {
        val presets = state.presets.map { stored ->
            val provider = LlmProviders.find(stored.provider) ?: LlmProviders.default
            Preset(
                stored.name,
                provider,
                stored.apiEndpoint.ifEmpty { provider.defaultEndpoint },
                stored.model.ifEmpty { provider.models.first().name },
                stored.apiKey,
            )
        }
        val active = state.active.coerceIn(0, (presets.size - 1).coerceAtLeast(0))
        return Presets(active, presets)
    }

    override suspend fun save(presets: Presets) {
        state = PluginPresetsState(
            active = presets.active,
            presets = presets.presets.map {
                StoredPreset(
                    name = it.name,
                    provider = it.provider.name,
                    apiEndpoint = it.apiEndpoint,
                    model = it.model,
                    apiKey = it.apiKey,
                )
            }.toMutableList()
        )
    }
}

