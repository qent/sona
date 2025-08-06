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

    @Tool("Switch agent role to Architect")
    override fun switchToArchitect() = internalTools.switchToArchitect()

    @Tool("Switch agent role to Code")
    override fun switchToCode() = internalTools.switchToCode()
}
