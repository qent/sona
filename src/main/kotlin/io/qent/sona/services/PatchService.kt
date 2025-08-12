package io.qent.sona.services

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
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

    private val logger = Logger.getInstance(PatchService::class.java)

    fun applyPatch(patchText: String) {
        logger.info("=== Starting patch application ===")
        logger.info("Patch text length: ${patchText.length}")
        logger.info("Project base path: ${project.basePath}")

        try {
            // Parse patch and resolve target file up-front
            logger.info("Parsing patch text...")
            val patches = PatchReader(patchText).readTextPatches()
            logger.info("Found ${patches.size} patches")

            val patch = patches.firstOrNull() ?: throw IllegalArgumentException(Strings.invalidPatch)
            logger.info("First patch - beforeName: '${patch.beforeName}', afterName: '${patch.afterName}'")
            logger.info("Patch hunks count: ${patch.hunks.size}")

            val relativePath =
                patch.afterName ?: patch.beforeName ?: throw IllegalArgumentException(Strings.invalidPatch)
            logger.info("Relative path: '$relativePath'")

            val fullPath = project.basePath?.let { Paths.get(it, relativePath).toString() } ?: relativePath
            logger.info("Full path: '$fullPath'")

            val file = LocalFileSystem.getInstance().findFileByPath(fullPath)
                ?: throw IllegalArgumentException("File not found: $relativePath")

            logger.info("File found: ${file.path}")
            logger.info("File exists: ${file.exists()}")
            logger.info("File is valid: ${file.isValid}")
            logger.info("File is writable: ${file.isWritable}")
            logger.info("File length: ${file.length}")

            val beforeName = patch.beforeName ?: relativePath
            val afterName = patch.afterName ?: relativePath
            logger.info("Final beforeName: '$beforeName', afterName: '$afterName'")

            // Build diff *on the EDT inside a single ReadAction* so UI sees the same snapshot
            ApplicationManager.getApplication().invokeLater {
                logger.info("=== Inside EDT invokeLater ===")
                logger.info("Is EDT: ${ApplicationManager.getApplication().isDispatchThread}")

                ReadAction.run<RuntimeException> {
                    logger.info("=== Inside ReadAction ===")
                    logger.info("Is read access allowed: ${ApplicationManager.getApplication().isReadAccessAllowed}")

                    val fileDocManager = FileDocumentManager.getInstance()
                    logger.info("Getting document for file...")

                    val doc = fileDocManager.getDocument(file)
                    logger.info("Document found: ${doc != null}")

                    if (doc != null) {
                        logger.info("Document length: ${doc.textLength}")
                        logger.info("Document is writable: ${doc.isWritable}")
                        logger.info("Document modification stamp: ${doc.modificationStamp}")
                        logger.info("File modification stamp: ${file.modificationStamp}")

                        // Check if document needs to be made writable
                        logger.info("Requesting writing permission...")
                        val wasWritable = doc.isWritable
                        fileDocManager.requestWriting(doc, project)
                        logger.info("Document writable before request: $wasWritable, after request: ${doc.isWritable}")

                        // Check document-file synchronization
                        if (doc.modificationStamp != file.modificationStamp) {
                            logger.warn("Document and file modification stamps don't match!")
                            logger.warn("Document stamp: ${doc.modificationStamp}, File stamp: ${file.modificationStamp}")
                        }
                    }

                    // IMPORTANT: do not normalize line separators — keep exact text as seen by IDE
                    val localText = if (doc != null) {
                        logger.info("Using document text")
                        doc.text
                    } else {
                        logger.info("Loading text from file directly")
                        LoadTextUtil.loadText(file).toString()
                    }

                    logger.info("Local text length: ${localText.length}")
                    logger.info(
                        "Local text first 100 chars: '${
                            localText.take(100).replace('\n', '\\').replace('\r', '|')
                        }'"
                    )

                    // Check for common line ending issues
                    val crlfCount = localText.count { it == '\r' }
                    val lfCount = localText.count { it == '\n' }
                    logger.info("Line endings - CR count: $crlfCount, LF count: $lfCount")

                    logger.info("Creating GenericPatchApplier...")
                    val applier = GenericPatchApplier(localText, patch.hunks)

                    logger.info("Executing patch application...")
                    val applyResult = applier.execute()
                    logger.info("Patch application result: $applyResult")

                    if (!applyResult) {
                        logger.error("Patch application failed!")
                        logger.error("Applied info: ${applier.appliedInfo}")
                        throw IllegalArgumentException(Strings.applyPatchFailed)
                    }

                    logger.info("Creating AppliedTextPatch...")
                    val appliedPatch = AppliedTextPatch.create(applier.appliedInfo)
                    logger.info("Applied patch created successfully")

                    logger.info("Patch result text length: ${applier.after.length}")
                    logger.info("Text changed: ${localText != applier.after}")

                    if (localText == applier.after) {
                        logger.warn("Patch didn't change the content - same text length and content")
                        logger.info("First 200 chars of original: '${localText.take(200)}'")
                        logger.info("First 200 chars of result: '${applier.after.take(200)}'")
                    }

                    logger.info("Creating result content...")

                    // Create writable content for patch result
                    val resultContent = try {
                        // Approach 1: Create simple text content (should be writable)
                        val simpleContent = DiffContentFactory.getInstance().create(applier.after)
                        logger.info("Simple content document is writable: ${simpleContent.document.isWritable}")
                        if (simpleContent.document.isWritable) {
                            simpleContent
                        } else {
                            // Force writable
                            ApplicationManager.getApplication().runWriteAction {
                                simpleContent.document.setReadOnly(false)
                            }
                            logger.info("Forced simple content to be writable: ${simpleContent.document.isWritable}")
                            simpleContent
                        }
                    } catch (e: Exception) {
                        logger.warn("Failed to create simple content: ${e.message}")
                        // Fallback to file-type specific content
                        DiffContentFactory.getInstance().create(project, applier.after, file.fileType)
                    }

                    logger.info("Result content type: ${resultContent.javaClass.simpleName}")

                    logger.info("Final result content document is writable: ${resultContent.document.isWritable}")

                    // If still not writable, force it
                    if (!resultContent.document.isWritable) {
                        logger.warn("Result content is still not writable, forcing...")
                        ApplicationManager.getApplication().runWriteAction {
                            resultContent.document.setReadOnly(false)
                        }
                        logger.info("After forcing: ${resultContent.document.isWritable}")
                    }

                    logger.info("Creating ApplyPatchDiffRequest...")
                    val request = ApplyPatchDiffRequest(
                        resultContent,
                        appliedPatch,
                        localText,
                        Strings.applyPatch,
                        beforeName,
                        afterName,
                        Strings.applyPatch
                    )

                    logger.info("Request created, showing diff...")
                    logger.info("Request title: ${request.title}")

                    try {
                        DiffManager.getInstance().showDiff(project, request)
                        logger.info("Diff dialog shown successfully")
                    } catch (e: Exception) {
                        logger.error("Error showing ApplyPatchDiffRequest dialog", e)
                        logger.info("Trying alternative approach with SimpleDiffRequest...")

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
                            logger.info("Alternative diff dialog shown successfully")
                        } catch (e2: Exception) {
                            logger.error("Alternative approach also failed", e2)
                            throw e  // Re-throw original exception
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error in applyPatch", e)
            logger.error("Exception type: ${e.javaClass.simpleName}")
            logger.error("Exception message: ${e.message}")
            logger.error("Stack trace: ", e)

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