package io.qent.sona.core.data

import dev.langchain4j.model.output.structured.Description

data class SearchResult(
    @Description("List of files matching the search query")
    val files: List<FileStructureInfo>,
    @Description("Map of file paths to line numbers where matches were found")
    val matchedLines: Map<String, Set<Int>>
)
