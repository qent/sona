package io.qent.sona.core

enum class DefaultRoles(val displayName: String) {
    ARCHITECT("\uD83C\uDFD7\uFE0F  Architect"),
    CODE("\uD83D\uDCBB  Code");

    companion object {
        val NAMES: Set<String> = entries.map { it.displayName }.toSet()
    }
}
