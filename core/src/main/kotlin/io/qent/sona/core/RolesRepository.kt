package io.qent.sona.core

interface RolesRepository {
    suspend fun load(): String
    suspend fun save(text: String)
}
