package io.qent.sona.services

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.project.Project
import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.openapi.diff.impl.patch.PatchReader
import com.intellij.openapi.diff.impl.patch.apply.GenericPatchApplier
import com.intellij.openapi.vcs.changes.patch.AppliedTextPatch
import com.intellij.openapi.vcs.changes.patch.tool.ApplyPatchDiffRequest
import com.intellij.openapi.vfs.LocalFileSystem
import java.nio.file.Paths
import io.qent.sona.Strings

@Service(Service.Level.PROJECT)
class PatchService(private val project: Project) {

    fun applyPatch(patchText: String) {
        try {
            // Parse patch and resolve target file up-front
            val patches = PatchReader(patchText).readTextPatches()
            val patch = patches.firstOrNull() ?: throw IllegalArgumentException(Strings.invalidPatch)

            val relativePath = patch.afterName ?: patch.beforeName ?: throw IllegalArgumentException(Strings.invalidPatch)
            val fullPath = project.basePath?.let { Paths.get(it, relativePath).toString() } ?: relativePath
            val file = LocalFileSystem.getInstance().findFileByPath(fullPath)
                ?: throw IllegalArgumentException("File not found: $relativePath")

            val beforeName = patch.beforeName ?: relativePath
            val afterName = patch.afterName ?: relativePath

            // Build diff *on the EDT inside a single ReadAction* so UI sees the same snapshot
            ApplicationManager.getApplication().invokeLater {
                ReadAction.run<RuntimeException> {
                    val doc = FileDocumentManager.getInstance().getDocument(file)?.also {
                        FileDocumentManager.getInstance().requestWriting(it, project)
                    }
                    // IMPORTANT: do not normalize line separators — keep exact text as seen by IDE
                    val localText = doc?.text ?: LoadTextUtil.loadText(file).toString()

                    val applier = GenericPatchApplier(localText, patch.hunks)
                    if (!applier.execute()) throw IllegalArgumentException(Strings.applyPatchFailed)

                    val appliedPatch = AppliedTextPatch.create(applier.appliedInfo)
                    val resultContent = DiffContentFactory.getInstance().create(project, applier.after, file.fileType)

                    val request = ApplyPatchDiffRequest(
                        resultContent,
                        appliedPatch,
                        localText,
                        Strings.applyPatch,
                        beforeName,
                        afterName,
                        Strings.applyPatch
                    )
                    DiffManager.getInstance().showDiff(project, request)
                }
            }
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
