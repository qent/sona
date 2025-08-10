package io.qent.sona.core.permissions

interface FilePermissionsRepository {
    val projectPath: String
    val whitelist: List<String>
    val blacklist: List<String>
}
