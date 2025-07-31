package io.qent.sona.core

data class Role(val name: String, val text: String)

data class Roles(
    val active: Int,
    val roles: List<Role>
)

interface RolesRepository {
    /** Load all roles with the active index. */
    suspend fun load(): Roles

    /** Persist the given roles data. */
    suspend fun save(roles: Roles)
}
