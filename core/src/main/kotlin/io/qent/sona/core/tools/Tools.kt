package io.qent.sona.core.tools

import io.qent.sona.core.permissions.DirectoryListing
import io.qent.sona.core.permissions.FileDependenciesInfo
import io.qent.sona.core.permissions.FileStructureInfo

interface Tools : InternalTools {
    fun getFocusedFileInfo(): FileStructureInfo
    fun getFileLines(path: String, fromLine: Int, toLine: Int): String
    fun readFile(path: String): String
    fun createPatch(patch: String): Int
    fun applyPatch(patchId: Int): String
    fun listPath(path: String): DirectoryListing
    fun sendTerminalCommand(command: String): String
    fun readTerminalOutput(): String
    fun getFileDependencies(path: String): FileDependenciesInfo
}
