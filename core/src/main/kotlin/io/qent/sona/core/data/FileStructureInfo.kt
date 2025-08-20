package io.qent.sona.core.data

import dev.langchain4j.model.output.structured.Description

data class FileStructureInfo(
    @Description("Absolute file path")
    val path: String,
    @Description("List of top-level elements in the file")
    val elements: List<FileElement>,
    @Description("Total number of lines in the file")
    val lineCount: Int,
)

data class FileElement(
    @Description("Element name")
    val name: String,
    @Description("Element type")
    val type: FileElementType,
    @Description("Whether the element is public")
    val public: Boolean,
    @Description("Start and end line numbers (1-based)")
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

