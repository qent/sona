package io.qent.sona.core.state.interactors

import io.qent.sona.core.roles.Role
import io.qent.sona.core.roles.Roles
import io.qent.sona.core.roles.RolesStateFlow

class EditRoleStateInteractor(private val flow: RolesStateFlow) {
    private var editingIndex: Int? = null
    var role: Role = Role("", "", "")
        private set

    val isNew: Boolean
        get() = editingIndex == null

    fun startCreate() {
        editingIndex = null
        role = Role("", "", "")
    }

    fun startEdit(idx: Int) {
        editingIndex = idx
        role = flow.value.roles[idx]
    }

    suspend fun save(r: Role) {
        val current = flow.value
        if (editingIndex == null) {
            flow.save(Roles(active = current.roles.size, roles = current.roles + r))
        } else {
            val list = current.roles.toMutableList()
            if (editingIndex!! in list.indices) {
                list[editingIndex!!] = r
                flow.save(current.copy(roles = list))
            }
        }
    }
}

