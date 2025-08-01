package io.qent.sona.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import io.qent.sona.PluginStateFlow

class OpenRolesAction : AnAction("Role", "Edit system message", AllIcons.General.User) {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<PluginStateFlow>()?.value?.onOpenRoles?.invoke()
    }
}
