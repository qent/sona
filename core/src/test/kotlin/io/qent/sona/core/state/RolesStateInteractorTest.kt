package io.qent.sona.core.state

import io.qent.sona.core.roles.Role
import io.qent.sona.core.roles.Roles
import io.qent.sona.core.roles.RolesRepository
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
        val repo = FakeRolesRepository(Roles(0, listOf(Role("A", "a"))))
        val interactor = RolesStateInteractor(repo)
        interactor.load()
        interactor.startCreateRole()
        interactor.addRole("B", "b")
        assertEquals(2, repo.data.roles.size)
        assertEquals(1, repo.data.active)
    }
}

