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

class RolesStateInteractorTest {
    @Test
    fun addRoleUpdatesRepository() = runBlocking {
        val repo = FakeRolesRepository(Roles(0, listOf(Role("A", "sa", "a"))))
        val flow = RolesStateFlow(repo)
        val interactor = RolesStateInteractor(flow)
        interactor.load()
        interactor.startCreateRole()
        interactor.addRole("B", "sb", "b")
        assertEquals(2, repo.data.roles.size)
        assertEquals(1, repo.data.active)
    }

    @Test
    fun selectRolePersistsActive() = runBlocking {
        val repo = FakeRolesRepository(Roles(0, listOf(Role("A", "sa", "a"), Role("B", "sb", "b"))))
        val flow = RolesStateFlow(repo)
        val interactor = RolesStateInteractor(flow)
        interactor.load()
        interactor.selectRole(1)
        assertEquals(1, repo.data.active)
    }

    @Test
    fun saveRoleUpdatesText() = runBlocking {
        val repo = FakeRolesRepository(Roles(0, listOf(Role("A", "sa", "a"))))
        val flow = RolesStateFlow(repo)
        val interactor = RolesStateInteractor(flow)
        interactor.load()
        interactor.saveRole("sn", "new")
        assertEquals("new", repo.data.roles[0].text)
        assertEquals("sn", repo.data.roles[0].short)
    }

    @Test
    fun deleteRoleSkipsDefault() = runBlocking {
        val repo = FakeRolesRepository(
            Roles(0, listOf(Role(DefaultRoles.ARCHITECTOR.displayName, "sa", "a"), Role("B", "sb", "b")))
        )
        val flow = RolesStateFlow(repo)
        val interactor = RolesStateInteractor(flow)
        interactor.load()
        interactor.deleteRole()
        assertEquals(2, repo.data.roles.size)
        interactor.selectRole(1)
        interactor.deleteRole()
        assertEquals(1, repo.data.roles.size)
    }

    @Test
    fun startAndFinishCreateRoleToggleFlag() = runBlocking {
        val repo = FakeRolesRepository(Roles(0, emptyList()))
        val flow = RolesStateFlow(repo)
        val interactor = RolesStateInteractor(flow)
        interactor.startCreateRole()
        assertEquals(true, interactor.creatingRole)
        interactor.finishCreateRole()
        assertEquals(false, interactor.creatingRole)
    }
}

