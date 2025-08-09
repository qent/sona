package io.qent.sona.core.roles

enum class DefaultRoles(val displayName: String) {
    ARCHITECTOR("Architector"),
    CODER("Coder");

    companion object {
        val NAMES: Set<String> = entries.map { it.displayName }.toSet()
    }
}
