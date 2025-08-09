package io.qent.sona.core.state

import io.qent.sona.core.roles.Role
import io.qent.sona.core.roles.Roles
import io.qent.sona.core.roles.RolesRepository
import io.qent.sona.core.roles.DefaultRoles
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakeRolesRepository(var data: Roles = Roles(0, emptyList())) : RolesRepository {
    override suspend fun load(): Roles = data
    override suspend fun save(roles: Roles) { data = roles }
}

class RolesListStateInteractorTest {
    @Test
    fun addRoleUpdatesRepository() = runBlocking {
        val repo = FakeRolesRepository(Roles(0, listOf(Role("A", "sa", "a"))))
        val flow = RolesStateFlow(repo)
        val interactor = RolesListStateInteractor(flow)
        interactor.load()
        interactor.addRole(Role("B", "sb", "b"))
        assertEquals(2, repo.data.roles.size)
        assertEquals(1, repo.data.active)
    }

    @Test
    fun selectRolePersistsActive() = runBlocking {
        val repo = FakeRolesRepository(Roles(0, listOf(Role("A", "sa", "a"), Role("B", "sb", "b"))))
        val flow = RolesStateFlow(repo)
        val interactor = RolesListStateInteractor(flow)
        interactor.load()
        interactor.selectRole(1)
        assertEquals(1, repo.data.active)
    }

    @Test
    fun updateRoleChangesData() = runBlocking {
        val repo = FakeRolesRepository(Roles(0, listOf(Role("A", "sa", "a"))))
        val flow = RolesStateFlow(repo)
        val interactor = RolesListStateInteractor(flow)
        interactor.load()
        interactor.updateRole(0, Role("A", "sn", "new"))
        assertEquals("new", repo.data.roles[0].text)
        assertEquals("sn", repo.data.roles[0].short)
    }

    @Test
    fun deleteRoleSkipsDefault() = runBlocking {
        val repo = FakeRolesRepository(
            Roles(0, listOf(Role(DefaultRoles.ARCHITECTOR.displayName, "sa", "a"), Role("B", "sb", "b")))
        )
        val flow = RolesStateFlow(repo)
        val interactor = RolesListStateInteractor(flow)
        interactor.load()
        interactor.deleteRole(0)
        assertEquals(2, repo.data.roles.size)
        interactor.deleteRole(1)
        assertEquals(1, repo.data.roles.size)
    }
}

class EditRoleStateInteractorTest {
    @Test
    fun startEditLoadsRole() = runBlocking {
        val repo = FakeRolesRepository(Roles(0, listOf(Role("A", "sa", "a"), Role("B", "sb", "b"))))
        val flow = RolesStateFlow(repo)
        flow.load()
        val editInteractor = EditRoleStateInteractor(flow)
        editInteractor.startEdit(1)
        assertEquals("B", editInteractor.role.name)
    }

    @Test
    fun saveUpdatesListInteractor() = runBlocking {
        val repo = FakeRolesRepository(Roles(0, listOf(Role("A", "sa", "a"))))
        val flow = RolesStateFlow(repo)
        flow.load()
        val editInteractor = EditRoleStateInteractor(flow)
        editInteractor.startCreate()
        editInteractor.save(Role("B", "sb", "b"))
        assertEquals(2, repo.data.roles.size)
    }
}

