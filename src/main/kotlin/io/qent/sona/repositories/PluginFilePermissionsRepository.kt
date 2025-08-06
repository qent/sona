package io.qent.sona.repositories

import com.intellij.openapi.project.Project
import io.qent.sona.config.SonaConfig
import io.qent.sona.core.permissions.FilePermissionsRepository

class PluginFilePermissionsRepository(project: Project) : FilePermissionsRepository {
    private val root = project.basePath ?: "/"
    private val config = SonaConfig.load(root)

    override val whitelist = config?.permissions?.files?.whitelist ?: listOf("$root/.*")
    override val blacklist = config?.permissions?.files?.blacklist ?: listOf(
        ".*/\\.env.*",
        ".*/\\.git.*",
        ".*/\\.idea/.*",
        ".*/gradle.properties",
        ".*/local\\.properties",
    )
}
