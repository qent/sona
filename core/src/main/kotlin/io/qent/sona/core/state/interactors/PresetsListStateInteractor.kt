package io.qent.sona.core.state.interactors

import io.qent.sona.core.presets.Preset
import io.qent.sona.core.presets.Presets
import io.qent.sona.core.presets.PresetsStateFlow

class PresetsListStateInteractor(private val flow: PresetsStateFlow) {

    suspend fun load(): Presets {
        flow.load()
        return flow.value
    }

    suspend fun selectPreset(idx: Int) {
        flow.save(flow.value.copy(active = idx))
    }

    suspend fun addPreset(preset: Preset) {
        val current = flow.value
        flow.save(Presets(active = current.presets.size, presets = current.presets + preset))
    }

    suspend fun updatePreset(idx: Int, preset: Preset) {
        val current = flow.value
        val list = current.presets.toMutableList()
        if (idx in list.indices) {
            list[idx] = preset
            flow.save(current.copy(presets = list))
        }
    }

    suspend fun deletePreset(idx: Int) {
        val current = flow.value
        if (current.presets.isEmpty() || idx !in current.presets.indices) return
        val list = current.presets.toMutableList()
        list.removeAt(idx)
        var active = current.active
        if (idx == active) {
            active = idx.coerceAtMost(list.lastIndex)
        } else if (idx < active) {
            active -= 1
        }
        if (list.isEmpty()) {
            active = 0
        }
        flow.save(Presets(active, list))
    }
}

