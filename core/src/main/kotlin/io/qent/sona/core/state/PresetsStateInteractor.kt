package io.qent.sona.core.state

import io.qent.sona.core.presets.Preset
import io.qent.sona.core.presets.Presets
import io.qent.sona.core.presets.PresetsRepository

class PresetsStateInteractor(private val repository: PresetsRepository) {
    var presets: Presets = Presets(0, emptyList())
        private set
    var creatingPreset: Boolean = false
        private set

    suspend fun load(): Presets {
        presets = repository.load()
        return presets
    }

    suspend fun selectPreset(idx: Int) {
        presets = presets.copy(active = idx)
        repository.save(presets)
    }

    fun startCreatePreset() {
        creatingPreset = true
    }

    fun finishCreatePreset() {
        creatingPreset = false
    }

    suspend fun addPreset(preset: Preset) {
        presets = Presets(active = presets.presets.size, presets = presets.presets + preset)
        creatingPreset = false
        repository.save(presets)
    }

    suspend fun deletePreset() {
        if (presets.presets.isEmpty()) return
        val list = presets.presets.toMutableList()
        list.removeAt(presets.active)
        val newActive = presets.active.coerceAtMost(list.lastIndex).coerceAtLeast(0)
        presets = Presets(newActive, list)
        repository.save(presets)
    }

    suspend fun savePreset(preset: Preset) {
        val list = presets.presets.toMutableList()
        if (list.isNotEmpty()) {
            list[presets.active] = preset
            presets = presets.copy(presets = list)
            repository.save(presets)
        }
    }
}

