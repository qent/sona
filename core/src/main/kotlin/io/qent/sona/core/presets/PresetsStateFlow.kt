package io.qent.sona.core.presets

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PresetsStateFlow(private val repository: PresetsRepository) : StateFlow<Presets> {
    private val _presets = MutableStateFlow(Presets(0, emptyList()))

    override val replayCache: List<Presets> get() = _presets.replayCache
    override val value: Presets get() = _presets.value
    override suspend fun collect(collector: FlowCollector<Presets>) = _presets.collect(collector)

    suspend fun load() {
        _presets.value = repository.load()
    }

    suspend fun save(presets: Presets) {
        _presets.value = presets
        repository.save(presets)
    }
}