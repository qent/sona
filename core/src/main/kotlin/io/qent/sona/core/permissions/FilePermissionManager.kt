package io.qent.sona.core.permissions

class FilePermissionManager(
    private val repository: FilePermissionsRepository,
) {
    fun getFileContent(fileInfo: FileInfo): String {
        val path = fileInfo.path
        val whitelisted = repository.whitelist.any { Regex(it).containsMatchIn(path) }
        val blacklisted = repository.blacklist.any { Regex(it).containsMatchIn(path) }
        return if (whitelisted && !blacklisted) {
            fileInfo.content
        } else {
            "Access to $path denied"
        }
    }
}
