package io.qent.sona.core.data

data class SearchResult(
    val files: List<FileStructureInfo>,
    val matchedLines: Map<String, Set<Int>>
)
