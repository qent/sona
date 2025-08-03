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
import io.qent.sona.core.State.ChatListState
import io.qent.sona.core.State.ChatState
import io.qent.sona.core.State.RolesState
import io.qent.sona.core.State.PresetsState
import io.qent.sona.ui.ChatPanel
import io.qent.sona.ui.PresetsPanel
import io.qent.sona.ui.RolesPanel
import io.qent.sona.ui.SonaTheme
import io.qent.sona.services.ThemeService
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
                        is ChatState -> ChatPanel(s)
                        is ChatListState -> ChatListPanel(s)
                        is RolesState -> RolesPanel(s)
                        is PresetsState -> PresetsPanel(s)
                    }
                }
            },
            null,
            false
        )
        toolWindow.contentManager.addContent(content)

        toolWindow.setTitleActions(listOf(
            ActionManager.getInstance().getAction("CreateNewChatAction"),
            ActionManager.getInstance().getAction("OpenHistoryAction"),
            ActionManager.getInstance().getAction("OpenRolesAction"),
            ActionManager.getInstance().getAction("OpenPresetsAction"),
        ))
    }
}