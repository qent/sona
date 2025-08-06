package io.qent.sona.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import io.qent.sona.PluginStateFlow
import io.qent.sona.core.state.State

class CreateNewChatAction : AnAction("Create", "Create new chat", AllIcons.General.Add) {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<PluginStateFlow>()?.lastState?.run {
            when (this) {
                is State.ChatListState -> onNewChat()
                is State.ChatState -> if (messages.isNotEmpty()) onNewChat()
                is State.RolesState -> onNewChat()
                is State.PresetsState -> onNewChat()
                is State.ServersState -> onNewChat()
            }
        }
    }
}