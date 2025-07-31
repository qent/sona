package io.qent.sona

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import io.qent.sona.ui.Ui
import org.jetbrains.jewel.bridge.JewelComposePanel

class PluginToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val content = ContentFactory.getInstance().createContent(
            JewelComposePanel { Ui(project) },
            null,
            false
        )
        toolWindow.contentManager.addContent(content)

        toolWindow.setTitleActions(listOf(
            ActionManager.getInstance().getAction("CreateNewChatAction"),
            ActionManager.getInstance().getAction("OpenHistoryAction")
        ))
    }
}