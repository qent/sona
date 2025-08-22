package io.qent.sona.repositories

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import io.qent.sona.config.SonaConfig
import io.qent.sona.core.permissions.FilePermissionsRepository
import kotlinx.coroutines.runBlocking

class PluginFilePermissionsRepository(private val project: Project) : FilePermissionsRepository {

    override val projectPath = project.basePath ?: "/"

    private fun role(): String = runBlocking {
        project.service<PluginRolesRepository>().load().let { it.roles[it.active].name }
    }

    private fun config() = SonaConfig.load(projectPath, role())

    override val whitelist: List<String>
        get() = config()?.permissions?.files?.whitelist ?: listOf("$projectPath/.*")

    override val blacklist: List<String>
        get() = config()?.permissions?.files?.blacklist ?: listOf(
            ".*/\\.env.*",
            ".*/\\.git.*",
            ".*/\\.idea/.*",
            ".*/gradle.properties",
            ".*/local\\.properties",
        )
}
