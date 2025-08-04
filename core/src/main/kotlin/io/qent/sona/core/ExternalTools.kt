package io.qent.sona.core

interface ExternalTools {
    fun getFocusedFileText(): FileInfo?
    fun readFile(path: String): FileInfo?
}
