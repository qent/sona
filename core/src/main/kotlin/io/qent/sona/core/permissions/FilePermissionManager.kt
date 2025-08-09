package io.qent.sona.core.permissions

class FilePermissionManager(
    private val repository: FilePermissionsRepository,
) {
    fun isFileAllowed(path: String): Boolean {
        val whitelisted = repository.whitelist.any { Regex(it).containsMatchIn(path) }
        val blacklisted = repository.blacklist.any { Regex(it).containsMatchIn(path) }
        return whitelisted && !blacklisted
    }

    fun getFileContent(fileInfo: FileInfo): String {
        val path = fileInfo.path
        return if (isFileAllowed(path)) {
            fileInfo.content
        } else {
            "Access to $path denied"
        }
    }
}
