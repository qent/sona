package io.qent.sona.core.tools

import dev.langchain4j.agent.tool.Tool
import io.qent.sona.core.data.DirectoryListing
import io.qent.sona.core.permissions.FilePermissionManager
import io.qent.sona.core.data.FileDependenciesInfo
import io.qent.sona.core.data.FileStructureInfo
import java.nio.file.Paths

import io.qent.sona.core.chat.ChatStateFlow

class ToolsInfoDecorator(
    private val chatStateFlow: ChatStateFlow,
    private val internalTools: InternalTools,
    private val externalTools: ExternalTools,
    private val filePermissionManager: FilePermissionManager,
) : Tools {

    @Tool("Return structure of file opened at current focused editor")
    override fun getFocusedFileInfo(): FileStructureInfo {
        val info = externalTools.getFocusedFileInfo() ?: return FileStructureInfo("", emptyList(), 0)
        return if (filePermissionManager.isFileAllowed(info.path)) info else FileStructureInfo("", emptyList(), 0)
    }

    @Tool("Return content of file at given absolute path and line range")
    override fun getFileLines(path: String, fromLine: Int, toLine: Int): String {
        val fileInfo = externalTools.getFileLines(path, fromLine, toLine) ?: return "File not found"
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

    @Tool("Apply a unified diff patch")
    override fun applyPatch(patch: String): String {
        val regex = Regex("^[-+]{3}\\s+(?:[ab]/)?(.+)", RegexOption.MULTILINE)
        val files = regex.findAll(patch).map { it.groupValues[1] }.toSet()
        for (file in files) {
            if (!filePermissionManager.isFileAllowed(file)) {
                throw IllegalArgumentException("Access to $file denied")
            }
        }
        return externalTools.applyPatch(chatStateFlow.currentState.chatId, patch)
    }

    override fun switchRole(name: String) = internalTools.switchRole(name)

    @Tool("Search the project using a free-text query. You can search by class/type name, plain text, file name, path/glob fragment, or a regex-like pattern in file contents or paths. Returns matching files (with structure info) and the set of matched line numbers per file.")
    override fun search(searchRequest: String) = internalTools.search(searchRequest)

    @Tool("Find file paths whose names match the pattern")
    override fun findFilesByNames(pattern: String, offset: Int, limit: Int): List<String> {
        return externalTools.findFilesByNames(pattern, offset, limit)
            .filter { filePermissionManager.isFileAllowed(it) }
    }

    @Tool("Find classes matching the pattern and return their structure")
    override fun findClasses(pattern: String, offset: Int, limit: Int): List<FileStructureInfo> {
        return externalTools.findClasses(pattern, offset, limit)
            .filter { filePermissionManager.isFileAllowed(it.path) }
    }

    @Tool("Find text occurrences matching the pattern")
    override fun findText(pattern: String, offset: Int, limit: Int): Map<String, Map<Int, String>> {
        return externalTools.findText(pattern, offset, limit)
            .filter { (path, _) -> filePermissionManager.isFileAllowed(path) }
    }

    @Tool("Execute a command in the IDE terminal")
    override fun sendTerminalCommand(command: String): String = externalTools.sendTerminalCommand(command)

    @Tool("Read all output from the IDE terminal")
    override fun readTerminalOutput(): String = externalTools.readTerminalOutput()
}
