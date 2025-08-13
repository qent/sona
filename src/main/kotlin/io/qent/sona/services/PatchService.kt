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
import com.intellij.openapi.components.service
import com.intellij.openapi.diff.impl.patch.PatchReader
import com.intellij.openapi.diff.impl.patch.apply.GenericPatchApplier
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.patch.AppliedTextPatch
import com.intellij.openapi.vcs.changes.patch.tool.ApplyPatchDiffRequest
import com.intellij.openapi.vfs.LocalFileSystem
import io.qent.sona.Strings
import io.qent.sona.repositories.PatchStorage
import java.nio.file.Paths

@Service(Service.Level.PROJECT)
class PatchService(private val project: Project) {

    fun createPatch(chatId: String, patchText: String): Int {
        val id = service<PatchStorage>().createPatch(chatId, patchText)

        try {
            // Parse patch and resolve target file up-front
            val patches = PatchReader(patchText).readTextPatches()

            val patch = patches.firstOrNull() ?: throw IllegalArgumentException(Strings.invalidPatch)

            val relativePath =
                patch.afterName ?: patch.beforeName ?: throw IllegalArgumentException(Strings.invalidPatch)

            val fullPath = project.basePath?.let { Paths.get(it, relativePath).toString() } ?: relativePath

            val file = LocalFileSystem.getInstance().findFileByPath(fullPath)
                ?: throw IllegalArgumentException("File not found: $relativePath")

            val beforeName = patch.beforeName ?: relativePath
            val afterName = patch.afterName ?: relativePath

            // Build diff *on the EDT inside a single ReadAction* so UI sees the same snapshot
            ApplicationManager.getApplication().invokeLater {
                ReadAction.run<RuntimeException> {
                    val fileDocManager = FileDocumentManager.getInstance()

                    val doc = fileDocManager.getDocument(file)

                    if (doc != null) {
                        fileDocManager.requestWriting(doc, project)
                    }

                    // IMPORTANT: do not normalize line separators — keep exact text as seen by IDE
                    val localText = doc?.text ?: LoadTextUtil.loadText(file).toString()

                    val applier = GenericPatchApplier(localText, patch.hunks)

                    val applyResult = applier.execute()

                    if (!applyResult) {
                        throw IllegalArgumentException(Strings.applyPatchFailed)
                    }

                    val appliedPatch = AppliedTextPatch.create(applier.appliedInfo)

                    // Create writable content for patch result
                    val resultContent = try {
                        // Approach 1: Create simple text content (should be writable)
                        val simpleContent = DiffContentFactory.getInstance().create(applier.after)
                        if (simpleContent.document.isWritable) {
                            simpleContent
                        } else {
                            // Force writable
                            ApplicationManager.getApplication().runWriteAction {
                                simpleContent.document.setReadOnly(false)
                            }
                            simpleContent
                        }
                    } catch (_: Exception) {
                        // Fallback to file-type specific content
                        DiffContentFactory.getInstance().create(project, applier.after, file.fileType)
                    }

                    // If still not writable, force it
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
                        // Alternative approach: use SimpleDiffRequest instead
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
                            throw e  // Re-throw original exception
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
        return id
    }

    fun applyPatch(chatId: String, patchId: Int): Boolean {
        val patchText = service<PatchStorage>().getPatch(chatId, patchId) ?: return false
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