package io.qent.sona.core.roles

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RolesStateFlow(private val repository: RolesRepository) : StateFlow<Roles> {
    private val _roles = MutableStateFlow(Roles(0, emptyList()))

    override val replayCache: List<Roles> get() = _roles.replayCache
    override val value: Roles get() = _roles.value
    override suspend fun collect(collector: FlowCollector<Roles>) = _roles.collect(collector)

    suspend fun load() {
        _roles.value = repository.load()
    }

    suspend fun save(roles: Roles) {
        _roles.value = roles
        repository.save(roles)
    }
}