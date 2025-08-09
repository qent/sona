package io.qent.sona.core.permissions

data class FileStructureInfo(
    val path: String,
    val elements: List<FileElement>,
)

data class FileElement(
    val name: String,
    val type: FileElementType,
    val public: Boolean,
    val lines: Pair<Int, Int>,
)

enum class FileElementType {
    CLASS,
    INTERFACE,
    OBJECT,
    ENUM,
    METHOD,
    FIELD,
}

