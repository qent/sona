package io.qent.sona.tools

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiManager
import io.qent.sona.Strings
import io.qent.sona.core.permissions.DirectoryListing
import io.qent.sona.core.permissions.FileDependenciesInfo
import io.qent.sona.core.permissions.FileInfo
import io.qent.sona.core.permissions.FileStructureInfo
import io.qent.sona.core.tools.ExternalTools
import io.qent.sona.services.PatchService
import io.qent.sona.tools.dependencies.JavaFileDependenciesProvider
import io.qent.sona.tools.dependencies.KotlinFileDependenciesProvider
import io.qent.sona.tools.structure.*
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.plugins.terminal.ShellTerminalWidget
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import java.nio.file.Files
import java.nio.file.Paths

class PluginExternalTools(private val project: Project) : ExternalTools {
    private val javaProvider = JavaFileStructureProvider()
    private val kotlinProvider = KotlinFileStructureProvider()
    private val pythonProvider = PythonFileStructureProvider()
    private val tsProvider = TypeScriptFileStructureProvider()
    private val jsProvider = JavaScriptFileStructureProvider()
    private val javaDepsProvider = JavaFileDependenciesProvider()
    private val kotlinDepsProvider = KotlinFileDependenciesProvider()
    private var terminalWidget: ShellTerminalWidget? = null

    override fun getFocusedFileInfo(): FileStructureInfo? {
        return runReadAction {
            val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return@runReadAction null
            val file = editor.virtualFile ?: return@runReadAction null
            val psiFile = PsiManager.getInstance(project).findFile(file) ?: return@runReadAction null
            val document = editor.document

            val provider = when (psiFile) {
                is KtFile -> kotlinProvider
                else -> when (psiFile.language.id.lowercase()) {
                    "java" -> javaProvider
                    "python" -> pythonProvider
                    "typescript" -> tsProvider
                    "javascript" -> jsProvider
                    else -> null
                }
            }
            val elements = provider?.collect(psiFile, document).orEmpty()
            FileStructureInfo(file.path, elements)
        }
    }

    override fun getFileLines(path: String, fromLine: Int, toLine: Int): FileInfo? {
        return try {
            val p = Paths.get(path)
            val lines = Files.readAllLines(p)
            val start = fromLine.coerceAtLeast(1) - 1
            val end = toLine.coerceAtMost(lines.size)
            val content = if (start < end) lines.subList(start, end).joinToString("\n") else ""
            FileInfo(p.toAbsolutePath().toString(), content)
        } catch (_: Exception) {
            null
        }
    }

    override fun readFile(path: String): FileInfo? {
        return try {
            val p = Paths.get(path)
            val content = Files.readString(p)
            FileInfo(p.toAbsolutePath().toString(), content)
        } catch (_: Exception) {
            null
        }
    }

    override fun createPatch(chatId: String, patch: String): Int {
        var id = 0
        val runnable = {
            id = project.service<PatchService>().createPatch(chatId, patch)
        }
        if (ApplicationManager.getApplication().isDispatchThread) {
            runnable()
        } else {
            ApplicationManager.getApplication().invokeAndWait { runnable() }
        }
        return id
    }

    override fun applyPatch(chatId: String, patchId: Int): String {
        val result = if (ApplicationManager.getApplication().isDispatchThread) {
            project.service<PatchService>().applyPatch(chatId, patchId)
        } else {
            var success = false
            ApplicationManager.getApplication().invokeAndWait {
                success = project.service<PatchService>().applyPatch(chatId, patchId)
            }
            success
        }
        return if (result) Strings.patchApplied else Strings.applyPatchFailed
    }

    override fun listPath(path: String): DirectoryListing? {
        return try {
            val p = Paths.get(path)
            val items = Files.list(p).use { stream ->
                stream.map { file ->
                    val name = file.fileName.toString()
                    if (Files.isDirectory(file)) "$name/" else name
                }.sorted().toList()
            }
            val contents = items.filter { it.endsWith("/") }.associateWith { dir ->
                val sub = p.resolve(dir.removeSuffix("/"))
                Files.list(sub).use { subStream ->
                    subStream.map { f ->
                        val name = f.fileName.toString()
                        if (Files.isDirectory(f)) "$name/" else name
                    }.sorted().toList()
                }
            }
            DirectoryListing(items, contents)
        } catch (_: Exception) {
            null
        }
    }

    override fun getFileDependencies(path: String): FileDependenciesInfo? {
        val vFile = LocalFileSystem.getInstance().findFileByPath(path) ?: return null
        return runReadAction {
            val psiFile = PsiManager.getInstance(project).findFile(vFile) ?: return@runReadAction null
            val provider = when (psiFile) {
                is KtFile -> kotlinDepsProvider
                else -> when (psiFile.language.id.lowercase()) {
                    "java" -> javaDepsProvider
                    else -> null
                }
            } ?: return@runReadAction null
            val deps = provider.collect(psiFile)
            FileDependenciesInfo(path, deps)
        }
    }

    private fun getTerminal(): ShellTerminalWidget {
        return if (ApplicationManager.getApplication().isDispatchThread) {
            getTerminalInternal()
        } else {
            var terminal: ShellTerminalWidget? = null
            ApplicationManager.getApplication().invokeAndWait {
                terminal = getTerminalInternal()
            }
            terminal ?: throw IllegalStateException("Failed to create terminal widget")
        }
    }

    private fun getTerminalInternal(): ShellTerminalWidget {
        val widget = terminalWidget
        if (widget != null && !Disposer.isDisposed(widget)) return widget
        val workingDir = project.basePath ?: System.getProperty("user.dir")
        val terminalManager = TerminalToolWindowManager.getInstance(project)
        val newWidget = terminalManager.createShellWidget(workingDir, "Sona", true, true)
        terminalWidget = newWidget as ShellTerminalWidget
        return newWidget
    }

    override fun sendTerminalCommand(command: String): String {
        if (ApplicationManager.getApplication().isDispatchThread) {
            getTerminal().executeCommand(command)
        } else {
            ApplicationManager.getApplication().invokeLater {
                getTerminal().executeCommand(command)
            }
        }
        return Strings.terminalCommandSent
    }

    override fun readTerminalOutput(): String {
        return getTerminal().terminalTextBuffer.toString()
    }
}
