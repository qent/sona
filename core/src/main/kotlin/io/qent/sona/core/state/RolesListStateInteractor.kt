package io.qent.sona.core.state

import io.qent.sona.core.roles.Role
import io.qent.sona.core.roles.Roles
import io.qent.sona.core.roles.DefaultRoles

class RolesListStateInteractor(private val flow: RolesStateFlow) {
    val roles: Roles
        get() = flow.value

    suspend fun load(): Roles {
        flow.load()
        return flow.value
    }

    suspend fun selectRole(idx: Int) {
        val r = flow.value.copy(active = idx)
        flow.save(r)
    }

    suspend fun selectRole(name: String) {
        val idx = flow.value.roles.indexOfFirst { it.name == name }
        if (idx >= 0) {
            selectRole(idx)
        }
    }

    suspend fun addRole(role: Role) {
        val current = flow.value
        val roles = Roles(active = current.roles.size, roles = current.roles + role)
        flow.save(roles)
    }

    suspend fun updateRole(idx: Int, role: Role) {
        val current = flow.value
        val list = current.roles.toMutableList()
        if (idx in list.indices) {
            list[idx] = role
            flow.save(current.copy(roles = list))
        }
    }

    suspend fun deleteRole(idx: Int) {
        val current = flow.value
        if (idx !in current.roles.indices) return
        if (current.roles[idx].name in DefaultRoles.NAMES) return
        val list = current.roles.toMutableList()
        list.removeAt(idx)
        val newActive = when {
            list.isEmpty() -> 0
            idx == current.active -> idx.coerceAtMost(list.lastIndex)
            idx < current.active -> current.active - 1
            else -> current.active
        }
        flow.save(Roles(active = newActive, roles = list))
    }
}

