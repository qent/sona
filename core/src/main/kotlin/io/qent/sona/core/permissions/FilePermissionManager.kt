package io.qent.sona.core.permissions

import io.qent.sona.core.data.FileLines

class FilePermissionManager(
    private val repository: FilePermissionsRepository,
) {
    fun isFileAllowed(path: String): Boolean {
        val fullPath = if (path.startsWith("/")) path else repository.projectPath + "/" + path
        val whitelisted = repository.whitelist.any { Regex(it).containsMatchIn(fullPath) }
        val blacklisted = repository.blacklist.any { Regex(it).containsMatchIn(fullPath) }
        return whitelisted && !blacklisted
    }

    fun getFileContent(fileInfo: FileLines): String {
        val path = fileInfo.path
        return if (isFileAllowed(path)) {
            fileInfo.content
        } else {
            "Access to $path denied"
        }
    }
}
