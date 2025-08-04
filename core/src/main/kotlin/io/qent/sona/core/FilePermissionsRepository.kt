package io.qent.sona.core

interface FilePermissionsRepository {
    val whitelist: List<String>
    val blacklist: List<String>
}
