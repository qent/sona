package io.qent.sona.core.tools

import dev.langchain4j.agent.tool.Tool
import io.qent.sona.core.permissions.DirectoryListing
import io.qent.sona.core.permissions.FilePermissionManager
import io.qent.sona.core.permissions.FileDependenciesInfo
import io.qent.sona.core.permissions.FileStructureInfo
import java.nio.file.Paths

class ToolsInfoDecorator(
    private val internalTools: InternalTools,
    private val externalTools: ExternalTools,
    private val filePermissionManager: FilePermissionManager,
) : Tools {

    @Tool("Return structure of file opened at current focused editor")
    override fun getFocusedFileInfo(): FileStructureInfo {
        val info = externalTools.getFocusedFileInfo() ?: return FileStructureInfo("", emptyList())
        return if (filePermissionManager.isFileAllowed(info.path)) info else FileStructureInfo("", emptyList())
    }

    @Tool("Return content of file at given absolute path and line range")
    override fun getFileLines(path: String, fromLine: Int, toLine: Int): String {
        val fileInfo = externalTools.getFileLines(path, fromLine, toLine) ?: return "File not found"
        return filePermissionManager.getFileContent(fileInfo)
    }

    @Tool("Return content of file at given absolute path")
    override fun readFile(path: String): String {
        val fileInfo = externalTools.readFile(path) ?: return "File not found"
        return filePermissionManager.getFileContent(fileInfo)
    }

    @Tool("Return list of files and directories at given absolute path with first-level contents")
    override fun listPath(path: String): DirectoryListing {
        if (!filePermissionManager.isFileAllowed("$path/")) {
            return DirectoryListing(emptyList(), emptyMap())
        }
        val listing = externalTools.listPath(path) ?: return DirectoryListing(emptyList(), emptyMap())
        fun allowed(parent: String, name: String): Boolean {
            val full = Paths.get(parent, name.removeSuffix("/")).toString()
            return filePermissionManager.isFileAllowed(full)
        }
        val items = listing.items.filter { allowed(path, it) }
        val contents = listing.contents
            .filter { (dir, _) -> allowed(path, dir) }
            .mapValues { (dir, list) ->
                val dirPath = Paths.get(path, dir).toString()
                list.filter { allowed(dirPath, it) }
            }
        return DirectoryListing(items, contents)
    }

    @Tool("Return dependencies of file at given absolute path")
    override fun getFileDependencies(path: String): FileDependenciesInfo {
        if (!filePermissionManager.isFileAllowed(path)) {
            return FileDependenciesInfo("", emptyList())
        }
        val info = externalTools.getFileDependencies(path) ?: return FileDependenciesInfo("", emptyList())
        val deps = info.dependencies.filter { filePermissionManager.isFileAllowed(it.path) }
        return FileDependenciesInfo(info.path, deps)
    }

    @Tool("Apply a unified diff patch text content to the project")
    override fun applyPatch(patch: String): String {
        val regex = Regex("^[-+]{3}\\s+(?:[ab]/)?(.+)", RegexOption.MULTILINE)
        val files = regex.findAll(patch).map { it.groupValues[1] }.toSet()
        for (file in files) {
            if (!filePermissionManager.isFileAllowed(file)) {
                return "Access to $file denied"
            }
        }
        return externalTools.applyPatch(patch)
    }

    override fun switchRole(name: String) = internalTools.switchRole(name)
}
