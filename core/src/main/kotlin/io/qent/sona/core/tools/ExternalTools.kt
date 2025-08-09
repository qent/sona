package io.qent.sona.core.tools

import io.qent.sona.core.permissions.FileInfo
import io.qent.sona.core.permissions.FileStructureInfo

interface ExternalTools {
    fun getFocusedFileInfo(): FileStructureInfo?
    fun getFileLines(path: String, fromLine: Int, toLine: Int): FileInfo?
    fun readFile(path: String): FileInfo?
    fun applyPatch(patch: String): String
}
