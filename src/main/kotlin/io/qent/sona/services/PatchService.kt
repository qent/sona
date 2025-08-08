package io.qent.sona.services

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.diff.impl.patch.PatchReader
import com.intellij.openapi.vcs.changes.patch.ApplyPatchDefaultExecutor
import com.intellij.openapi.vcs.changes.patch.ApplyPatchDifferentiatedDialog
import com.intellij.openapi.vcs.changes.patch.ApplyPatchMode
import io.qent.sona.Strings

@Service(Service.Level.PROJECT)
class PatchService(private val project: Project) {

    fun applyPatch(patchText: String) {
        ApplicationManager.getApplication().invokeLater {
            try {
                val patches = PatchReader(patchText).readTextPatches()
                val executor = ApplyPatchDefaultExecutor(project)
                ApplyPatchDifferentiatedDialog(
                    project,
                    executor,
                    listOf(executor),
                    ApplyPatchMode.APPLY,
                    patches,
                    null
                ).show()
            } catch (e: Exception) {
                Notifications.Bus.notify(
                    Notification(
                        "Sona",
                        Strings.applyPatchFailed,
                        e.message ?: Strings.invalidPatch,
                        NotificationType.ERROR
                    ),
                    project
                )
            }
        }
    }
}

