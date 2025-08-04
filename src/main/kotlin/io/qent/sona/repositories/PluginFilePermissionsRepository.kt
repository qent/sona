package io.qent.sona.repositories

import com.intellij.openapi.project.Project
import io.qent.sona.core.FilePermissionsRepository

class PluginFilePermissionsRepository(project: Project) : FilePermissionsRepository {
    private val root = project.basePath ?: "/"
    override val whitelist = listOf("$root/.*")
    override val blacklist = listOf(
        ".*/\\.env.*",
        ".*/\\.git.*",
        ".*/\\.idea/.*",
        ".*/gradle.properties",
        ".*/local\\.properties",
    )
}
