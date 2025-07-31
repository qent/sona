package io.qent.sona.core

import dev.langchain4j.agent.tool.Tool

class ToolsInfoDecorator(private val tools: Tools) : Tools {

    @Tool("Return source of file opened at current focused editor")
    override fun getFocusedFileText() = tools.getFocusedFileText()

    @Tool("Switch agent role to Architect")
    override fun switchToArchitect() = tools.switchToArchitect()

    @Tool("Switch agent role to Code")
    override fun switchToCode() = tools.switchToCode()
}
