package io.qent.sona.core.state

import io.qent.sona.core.presets.LlmProviders
import io.qent.sona.core.presets.Preset
import io.qent.sona.core.presets.Presets

class EditPresetStateInteractor(private val flow: PresetsStateFlow) {
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
        preset = flow.value.presets[idx]
    }

    suspend fun save(p: Preset) {
        val current = flow.value
        if (editingIndex == null) {
            flow.save(Presets(active = current.presets.size, presets = current.presets + p))
        } else {
            val list = current.presets.toMutableList()
            if (editingIndex!! in list.indices) {
                list[editingIndex!!] = p
                flow.save(current.copy(presets = list))
            }
        }
    }
}

