package io.qent.sona.core.permissions

/** Information about dependencies of a file. */
data class FileDependenciesInfo(
    val path: String,
    val dependencies: List<FileDependency>,
)

/** Represents a single dependency and the file that defines it. */
data class FileDependency(
    val name: String,
    val path: String,
)

