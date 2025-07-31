package io.qent.sona

import ChatListPanel
import androidx.compose.runtime.collectAsState
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import io.qent.sona.core.State.ChatListState
import io.qent.sona.core.State.ChatState
import io.qent.sona.core.State.RolesState
import io.qent.sona.ui.ChatPanel
import io.qent.sona.ui.RolesPanel
import org.jetbrains.jewel.bridge.JewelComposePanel

class PluginToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val content = ContentFactory.getInstance().createContent(
            JewelComposePanel {
                val pluginStateFlow = project.service<PluginStateFlow>()
                val state = pluginStateFlow.collectAsState(pluginStateFlow.lastState)
                when (val s = state.value) {
                    is ChatState -> ChatPanel(s)
                    is ChatListState -> ChatListPanel(s)
                    is RolesState -> RolesPanel(s)
                }
          },
            null,
            false
        )
        toolWindow.contentManager.addContent(content)

        toolWindow.setTitleActions(listOf(
            ActionManager.getInstance().getAction("CreateNewChatAction"),
            ActionManager.getInstance().getAction("OpenHistoryAction"),
            ActionManager.getInstance().getAction("OpenRolesAction")
        ))
    }
}