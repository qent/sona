package io.qent.sona.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import io.qent.sona.PluginStateFlow
import io.qent.sona.Strings

class OpenPresetsAction : AnAction(Strings.presetsAction, Strings.presetsActionDescription, AllIcons.General.GearPlain) {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<PluginStateFlow>()?.lastState?.onOpenPresets?.invoke()
    }
}
