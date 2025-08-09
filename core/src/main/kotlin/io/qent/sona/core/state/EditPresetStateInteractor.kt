package io.qent.sona.core.state

import io.qent.sona.core.presets.LlmProviders
import io.qent.sona.core.presets.Preset

class EditPresetStateInteractor(private val listInteractor: PresetsListStateInteractor) {
    private var editingIndex: Int? = null
    var preset: Preset = defaultPreset()
        private set

    val isNew: Boolean
        get() = editingIndex == null

    private fun defaultPreset(): Preset {
        val provider = LlmProviders.default
        val model = provider.models.firstOrNull()?.name ?: ""
        return Preset(
            name = model,
            provider = provider,
            apiEndpoint = provider.defaultEndpoint,
            model = model,
            apiKey = "",
        )
    }

    fun startCreate() {
        editingIndex = null
        preset = defaultPreset()
    }

    fun startEdit(idx: Int) {
        editingIndex = idx
        preset = listInteractor.presets.presets[idx]
    }

    suspend fun save(p: Preset) {
        if (editingIndex == null) {
            listInteractor.addPreset(p)
        } else {
            listInteractor.updatePreset(editingIndex!!, p)
        }
    }
}

