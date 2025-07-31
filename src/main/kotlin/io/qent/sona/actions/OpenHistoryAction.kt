package io.qent.sona.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import io.qent.sona.PluginStateFlow

class OpenHistoryAction: AnAction("History", "Open chat history", AllIcons.General.History) {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<PluginStateFlow>()?.value?.onOpenHistory()
    }
}