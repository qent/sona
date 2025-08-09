package io.qent.sona.core.state

import io.qent.sona.core.roles.Role
import io.qent.sona.core.roles.Roles
import io.qent.sona.core.roles.RolesRepository
import io.qent.sona.core.roles.DefaultRoles

class RolesStateInteractor(private val repository: RolesRepository) {
    var roles: Roles = Roles(0, emptyList())
        private set
    var creatingRole: Boolean = false
        private set

    suspend fun load(): Roles {
        roles = repository.load()
        return roles
    }

    suspend fun selectRole(idx: Int) {
        roles = roles.copy(active = idx)
        repository.save(roles)
    }

    suspend fun selectRole(name: String) {
        val idx = roles.roles.indexOfFirst { it.name == name }
        if (idx >= 0) {
            selectRole(idx)
        }
    }

    suspend fun saveRole(short: String, text: String) {
        val list = roles.roles.toMutableList()
        list[roles.active] = list[roles.active].copy(short = short, text = text)
        roles = roles.copy(roles = list)
        repository.save(roles)
    }

    fun startCreateRole() {
        creatingRole = true
    }

    fun finishCreateRole() {
        creatingRole = false
    }

    suspend fun addRole(name: String, short: String, text: String) {
        roles = Roles(active = roles.roles.size, roles = roles.roles + Role(name, short, text))
        creatingRole = false
        repository.save(roles)
    }

    suspend fun deleteRole() {
        val currentName = roles.roles[roles.active].name
        if (currentName in DefaultRoles.NAMES) return
        val list = roles.roles.toMutableList()
        list.removeAt(roles.active)
        val newActive = roles.active.coerceAtMost(list.lastIndex)
        roles = Roles(active = newActive, roles = list)
        repository.save(roles)
    }
}

