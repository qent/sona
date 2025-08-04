package io.qent.sona.core

class FilePermissionManager(
    private val repository: FilePermissionsRepository,
) {
    fun getFileContent(fileInfo: FileInfo): String {
        val path = fileInfo.path
        val whitelisted = repository.whitelist.any { Regex(it).matches(path) }
        val blacklisted = repository.blacklist.any { Regex(it).matches(path) }
        return if (whitelisted && !blacklisted) {
            fileInfo.content
        } else {
            "Access to $path denied"
        }
    }
}
