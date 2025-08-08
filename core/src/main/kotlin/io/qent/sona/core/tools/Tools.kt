package io.qent.sona.core.tools

interface Tools : InternalTools {
    fun getFocusedFileText(): String
    fun readFile(path: String): String
    fun applyPatch(patch: String): String
}
