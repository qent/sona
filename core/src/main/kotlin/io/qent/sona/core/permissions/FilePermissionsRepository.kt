package io.qent.sona.core.permissions

interface FilePermissionsRepository {
    val whitelist: List<String>
    val blacklist: List<String>
}
