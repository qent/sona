package io.qent.sona.core.permissions

data class DirectoryListing(
    val items: List<String>,
    val contents: Map<String, List<String>>
)
