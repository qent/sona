package io.qent.sona.core.tools

import dev.langchain4j.agent.tool.Tool
import io.qent.sona.core.permissions.FilePermissionManager

class ToolsInfoDecorator(
    private val internalTools: InternalTools,
    private val externalTools: ExternalTools,
    private val filePermissionManager: FilePermissionManager,
) : Tools {

    @Tool("Return source of file opened at current focused editor")
    override fun getFocusedFileText(): String {
        val fileInfo = externalTools.getFocusedFileText() ?: return ""
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
            val check = filePermissionManager.getFileContent(io.qent.sona.core.permissions.FileInfo(file, ""))
            if (check == "Access to $file denied") {
                return check
            }
        }
        return externalTools.applyPatch(patch)
    }

    @Tool("Switch agent role to Architect")
    override fun switchToArchitect() = internalTools.switchToArchitect()

    @Tool("Switch agent role to Code")
    override fun switchToCode() = internalTools.switchToCode()
}
