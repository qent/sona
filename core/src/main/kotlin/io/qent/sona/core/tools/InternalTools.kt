package io.qent.sona.core.tools

import io.qent.sona.core.data.SearchResult

interface InternalTools {
    fun switchRole(name: String): String
    fun search(searchRequest: String): List<SearchResult>
}
