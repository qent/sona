package io.qent.sona.core.tools

import dev.langchain4j.agent.tool.Tool
import io.qent.sona.core.permissions.FilePermissionManager
import io.qent.sona.core.permissions.FileStructureInfo

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

    @Tool("Apply a unified diff patch to the project")
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
