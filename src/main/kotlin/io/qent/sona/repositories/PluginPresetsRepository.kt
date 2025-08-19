package io.qent.sona.repositories

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
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
        val passwordSafe = PasswordSafe.instance
        val presets = state.presets.map { stored ->
            val provider = LlmProviders.find(stored.provider) ?: LlmProviders.default
            val apiKey = passwordSafe.getPassword(credentials(stored.name)) ?: ""
            Preset(
                stored.name,
                provider,
                stored.apiEndpoint.ifEmpty { provider.defaultEndpoint },
                stored.model.ifEmpty { provider.models.firstOrNull()?.name ?: "" },
                apiKey,
            )
        }
        val active = state.active.coerceIn(0, (presets.size - 1).coerceAtLeast(0))
        return Presets(active, presets)
    }

    override suspend fun save(presets: Presets) {
        val passwordSafe = PasswordSafe.instance

        val existingNames = state.presets.map { it.name }.toSet()
        val newNames = presets.presets.map { it.name }.toSet()
        for (deleted in existingNames - newNames) {
            passwordSafe.setPassword(credentials(deleted), null)
        }

        presets.presets.forEach { preset ->
            passwordSafe.setPassword(credentials(preset.name), preset.apiKey)
        }

        state = PluginPresetsState(
            active = presets.active,
            presets = presets.presets.map {
                StoredPreset(
                    name = it.name,
                    provider = it.provider.name,
                    apiEndpoint = it.apiEndpoint,
                    model = it.model,
                )
            }.toMutableList()
        )
    }

    private fun credentials(name: String): CredentialAttributes =
        CredentialAttributes(generateServiceName("Sona Preset", name))
}

