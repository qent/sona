package io.qent.sona.repositories

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import io.qent.sona.core.LlmProvider
import io.qent.sona.core.Preset
import io.qent.sona.core.Presets
import io.qent.sona.core.PresetsRepository

@Service
@State(name = "PluginPresets", storages = [Storage("presets.xml")])
class PluginPresetsRepository : PresetsRepository,
    PersistentStateComponent<PluginPresetsRepository.PluginPresetsState> {

    data class StoredPreset(
        var name: String = "",
        var provider: String = LlmProvider.Anthropic.name,
        var apiEndpoint: String = LlmProvider.Anthropic.defaultEndpoint,
        var model: String = LlmProvider.Anthropic.models.first().name,
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
            val provider = runCatching { LlmProvider.valueOf(stored.provider) }.getOrDefault(LlmProvider.Anthropic)
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

