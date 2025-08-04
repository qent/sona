package io.qent.sona.core

import dev.langchain4j.agent.tool.Tool

class ToolsInfoDecorator(
    private val internalTools: InternalTools,
    private val externalTools: ExternalTools,
) : Tools {

    @Tool("Return source of file opened at current focused editor")
    override fun getFocusedFileText() = externalTools.getFocusedFileText()

    @Tool("Switch agent role to Architect")
    override fun switchToArchitect() = internalTools.switchToArchitect()

    @Tool("Switch agent role to Code")
    override fun switchToCode() = internalTools.switchToCode()
}
