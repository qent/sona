package io.qent.sona.core

interface Tools : InternalTools {
    fun getFocusedFileText(): String
    fun readFile(path: String): String
}
