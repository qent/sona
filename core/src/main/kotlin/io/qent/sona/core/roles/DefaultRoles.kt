package io.qent.sona.core.roles

enum class DefaultRoles(val displayName: String) {
    ARCHITECTOR("Architector"),
    CODER("Coder"),
    MANAGER("Manager");

    companion object {
        val NAMES: Set<String> = entries.map { it.displayName }.toSet()
    }
}
