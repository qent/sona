package io.qent.sona.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.wm.ToolWindowManager

class OpenSonaAction : AnAction("Open Sona", "Open chat with Sona", null), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ToolWindowManager.getInstance(project).getToolWindow("Sona")?.also { toolWindow ->
            if (toolWindow.isVisible) {
                toolWindow.hide()
            } else {
                toolWindow.show()
            }
        }
    }
}