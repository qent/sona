package io.qent.sona.services

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diff.impl.patch.PatchReader
import com.intellij.openapi.diff.impl.patch.apply.GenericPatchApplier
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.patch.AppliedTextPatch
import com.intellij.openapi.vcs.changes.patch.tool.ApplyPatchDiffRequest
import com.intellij.openapi.vfs.LocalFileSystem
import io.qent.sona.Strings
import java.nio.file.Paths

@Service(Service.Level.PROJECT)
class PatchService(private val project: Project) {

    fun showPatchDiff(patchText: String) {
        try {
            val patches = PatchReader(patchText).readTextPatches()
            val patch = patches.firstOrNull() ?: throw IllegalArgumentException(Strings.invalidPatch)
            val relativePath =
                patch.afterName ?: patch.beforeName ?: throw IllegalArgumentException(Strings.invalidPatch)
            val fullPath = project.basePath?.let { Paths.get(it, relativePath).toString() } ?: relativePath
            val file = LocalFileSystem.getInstance().findFileByPath(fullPath)
                ?: throw IllegalArgumentException("File not found: $relativePath")
            val beforeName = patch.beforeName ?: relativePath
            val afterName = patch.afterName ?: relativePath

            ApplicationManager.getApplication().invokeLater {
                ReadAction.run<RuntimeException> {
                    val fileDocManager = FileDocumentManager.getInstance()
                    val doc = fileDocManager.getDocument(file)
                    if (doc != null) {
                        fileDocManager.requestWriting(doc, project)
                    }
                    val localText = doc?.text ?: LoadTextUtil.loadText(file).toString()
                    val applier = GenericPatchApplier(localText, patch.hunks)
                    val applyResult = applier.execute()
                    if (!applyResult) {
                        throw IllegalArgumentException(Strings.applyPatchFailed)
                    }
                    val appliedPatch = AppliedTextPatch.create(applier.appliedInfo)
                    val resultContent = try {
                        val simpleContent = DiffContentFactory.getInstance().create(applier.after)
                        if (simpleContent.document.isWritable) {
                            simpleContent
                        } else {
                            ApplicationManager.getApplication().runWriteAction {
                                simpleContent.document.setReadOnly(false)
                            }
                            simpleContent
                        }
                    } catch (_: Exception) {
                        DiffContentFactory.getInstance().create(project, applier.after, file.fileType)
                    }
                    if (!resultContent.document.isWritable) {
                        ApplicationManager.getApplication().runWriteAction {
                            resultContent.document.setReadOnly(false)
                        }
                    }
                    val request = ApplyPatchDiffRequest(
                        resultContent,
                        appliedPatch,
                        localText,
                        Strings.applyPatch,
                        beforeName,
                        afterName,
                        Strings.applyPatch
                    )
                    try {
                        DiffManager.getInstance().showDiff(project, request)
                    } catch (e: Exception) {
                        try {
                            val originalContent =
                                DiffContentFactory.getInstance().create(project, localText, file.fileType)
                            val modifiedContent =
                                DiffContentFactory.getInstance().create(project, applier.after, file.fileType)
                            val simpleDiffRequest = SimpleDiffRequest(
                                Strings.applyPatch,
                                originalContent,
                                modifiedContent,
                                beforeName,
                                afterName
                            )
                            DiffManager.getInstance().showDiff(project, simpleDiffRequest)
                        } catch (_: Exception) {
                            throw e
                        }
                    }
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

    fun applyPatch(patchText: String): Boolean {
        return try {
            val patches = PatchReader(patchText).readTextPatches()
            val patch = patches.firstOrNull() ?: throw IllegalArgumentException(Strings.invalidPatch)
            val relativePath =
                patch.afterName ?: patch.beforeName ?: throw IllegalArgumentException(Strings.invalidPatch)
            val fullPath = project.basePath?.let { Paths.get(it, relativePath).toString() } ?: relativePath
            val file = LocalFileSystem.getInstance().findFileByPath(fullPath)
                ?: throw IllegalArgumentException("File not found: $relativePath")
            val fileDocManager = FileDocumentManager.getInstance()
            val doc = fileDocManager.getDocument(file)
            val localText = doc?.text ?: LoadTextUtil.loadText(file).toString()
            val applier = GenericPatchApplier(localText, patch.hunks)
            val applyResult = applier.execute()
            if (!applyResult) throw IllegalArgumentException(Strings.applyPatchFailed)
            ApplicationManager.getApplication().invokeAndWait {
                WriteCommandAction.runWriteCommandAction(project) {
                    val document = doc ?: fileDocManager.getDocument(file)
                    if (document != null) {
                        document.setText(applier.after)
                        fileDocManager.saveDocument(document)
                    }
                }
            }
            true
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
            false
        }
    }
}

