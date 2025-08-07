package io.qent.sona.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import io.qent.sona.PluginStateFlow
import io.qent.sona.Strings

class OpenServersAction : AnAction(Strings.serversAction, Strings.serversActionDescription, AllIcons.Webreferences.Server) {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<PluginStateFlow>()?.lastState?.onOpenServers?.invoke()
    }
}
