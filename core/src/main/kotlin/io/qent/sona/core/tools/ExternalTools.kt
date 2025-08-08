package io.qent.sona.core.tools

import io.qent.sona.core.permissions.FileInfo

interface ExternalTools {
    fun getFocusedFileText(): FileInfo?
    fun readFile(path: String): FileInfo?
    fun applyPatch(patch: String): String
}
