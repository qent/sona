package io.qent.sona.core.tools

import io.qent.sona.core.permissions.DirectoryListing
import io.qent.sona.core.permissions.FileStructureInfo

interface Tools : InternalTools {
    fun getFocusedFileInfo(): FileStructureInfo
    fun getFileLines(path: String, fromLine: Int, toLine: Int): String
    fun readFile(path: String): String
    fun applyPatch(patch: String): String
    fun listPath(path: String): DirectoryListing
}
