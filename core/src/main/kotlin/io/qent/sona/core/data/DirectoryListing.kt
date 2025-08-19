package io.qent.sona.core.data

data class DirectoryListing(
    val items: List<String>,
    val contents: Map<String, List<String>>
)
