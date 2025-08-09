package io.qent.sona.core.state

import io.qent.sona.core.presets.Preset
import io.qent.sona.core.presets.Presets
import io.qent.sona.core.presets.PresetsRepository

class PresetsListStateInteractor(private val repository: PresetsRepository) {
    var presets: Presets = Presets(0, emptyList())
        private set

    suspend fun load(): Presets {
        presets = repository.load()
        return presets
    }

    suspend fun selectPreset(idx: Int) {
        presets = presets.copy(active = idx)
        repository.save(presets)
    }

    suspend fun addPreset(preset: Preset) {
        presets = Presets(active = presets.presets.size, presets = presets.presets + preset)
        repository.save(presets)
    }

    suspend fun updatePreset(idx: Int, preset: Preset) {
        val list = presets.presets.toMutableList()
        if (idx in list.indices) {
            list[idx] = preset
            presets = presets.copy(presets = list)
            repository.save(presets)
        }
    }

    suspend fun deletePreset(idx: Int) {
        if (presets.presets.isEmpty() || idx !in presets.presets.indices) return
        val list = presets.presets.toMutableList()
        list.removeAt(idx)
        var active = presets.active
        if (idx == active) {
            active = idx.coerceAtMost(list.lastIndex)
        } else if (idx < active) {
            active -= 1
        }
        if (list.isEmpty()) {
            active = 0
        }
        presets = Presets(active, list)
        repository.save(presets)
    }
}

