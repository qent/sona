package io.qent.sona.core.state

import io.qent.sona.core.roles.Role

class EditRoleStateInteractor(private val listInteractor: RolesListStateInteractor) {
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
        role = listInteractor.roles.roles[idx]
    }

    suspend fun save(r: Role) {
        if (editingIndex == null) {
            listInteractor.addRole(r)
        } else {
            listInteractor.updateRole(editingIndex!!, r)
        }
    }
}

