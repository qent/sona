package io.qent.sona.services

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.openapi.diff.impl.patch.PatchReader
import com.intellij.openapi.diff.impl.patch.apply.GenericPatchApplier
import com.intellij.openapi.vcs.changes.patch.AppliedTextPatch
import com.intellij.openapi.vcs.changes.patch.tool.ApplyPatchDiffRequest
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import java.nio.file.Paths
import io.qent.sona.Strings

@Service(Service.Level.PROJECT)
class PatchService(private val project: Project) {

    fun applyPatch(patchText: String) {
        ApplicationManager.getApplication().invokeLater {
            try {
                val patches = PatchReader(patchText).readTextPatches()
                val patch = patches.firstOrNull() ?: throw IllegalArgumentException(Strings.invalidPatch)
                val relativePath = patch.afterName ?: patch.beforeName ?: throw IllegalArgumentException(Strings.invalidPatch)
                val fullPath = project.basePath?.let { Paths.get(it, relativePath).toString() } ?: relativePath
                val file = LocalFileSystem.getInstance().findFileByPath(fullPath)
                    ?: throw IllegalArgumentException("File not found: $relativePath")
                val localContent = VfsUtil.loadText(file)
                val applier = GenericPatchApplier(localContent, patch.hunks)
                if (!applier.execute()) throw IllegalArgumentException(Strings.applyPatchFailed)
                val appliedPatch = AppliedTextPatch.create(applier.appliedInfo)
                val resultContent = DiffContentFactory.getInstance().create(project, applier.after, file.fileType)
                val request = ApplyPatchDiffRequest(
                    resultContent,
                    appliedPatch,
                    localContent,
                    Strings.applyPatch,
                    relativePath,
                    relativePath,
                    Strings.applyPatch
                )
                DiffManager.getInstance().showDiff(project, request)
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

