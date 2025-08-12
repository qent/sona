package io.qent.sona.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import io.qent.sona.PluginStateFlow
import io.qent.sona.Strings

class OpenUserPromptAction : AnAction(Strings.userSystemPromptAction, Strings.userSystemPromptActionDescription, AllIcons.FileTypes.Text) {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<PluginStateFlow>()?.lastState?.onOpenUserPrompt?.invoke()
    }
}
