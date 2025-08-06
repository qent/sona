package io.qent.sona

import ChatListPanel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import io.qent.sona.core.State.*
import io.qent.sona.services.ThemeService
import io.qent.sona.ui.chat.ChatPanel
import io.qent.sona.ui.presets.PresetsPanel
import io.qent.sona.ui.roles.RolesPanel
import io.qent.sona.ui.mcp.ServersPanel
import io.qent.sona.ui.SonaTheme
import org.jetbrains.jewel.bridge.JewelComposePanel

class PluginToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val content = ContentFactory.getInstance().createContent(
            JewelComposePanel {
                val themeService = service<ThemeService>()
                val dark by themeService.isDark.collectAsState()
                val pluginStateFlow = project.service<PluginStateFlow>()
                val state = pluginStateFlow.collectAsState(pluginStateFlow.lastState)
                SonaTheme(dark = dark) {
                    when (val s = state.value) {
                        is ChatState -> ChatPanel(project, s)
                        is ChatListState -> ChatListPanel(s)
                        is RolesState -> RolesPanel(s)
                        is PresetsState -> PresetsPanel(s)
                        is ServersState -> ServersPanel(s)
                    }
                }
            },
            null,
            false
        )
        toolWindow.contentManager.addContent(content)

        toolWindow.setTitleActions(
            listOf(
                ActionManager.getInstance().getAction("CreateNewChatAction"),
                ActionManager.getInstance().getAction("OpenHistoryAction"),
                ActionManager.getInstance().getAction("OpenRolesAction"),
                ActionManager.getInstance().getAction("OpenPresetsAction"),
                ActionManager.getInstance().getAction("OpenServersAction"),
            )
        )
    }
}