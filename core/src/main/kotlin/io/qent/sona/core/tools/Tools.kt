package io.qent.sona.core.tools

import io.qent.sona.core.data.DirectoryListing
import io.qent.sona.core.data.FileDependenciesInfo
import io.qent.sona.core.data.FileStructureInfo

interface Tools : InternalTools {
    fun getFocusedFileInfo(): FileStructureInfo
    fun getFileLines(path: String, fromLine: Int, toLine: Int): String
    fun applyPatch(patch: String): String
    fun listPath(path: String): DirectoryListing
    fun sendTerminalCommand(command: String): String
    fun readTerminalOutput(): String
    fun getFileDependencies(path: String): FileDependenciesInfo
    fun findFilesByNames(pattern: String, offset: Int = 0, limit: Int = 3): List<String>
    fun findClasses(pattern: String, offset: Int = 0, limit: Int = 3): List<FileStructureInfo>
    fun findText(pattern: String, offset: Int = 0, limit: Int = 3): Map<String, Map<Int, String>>
}
