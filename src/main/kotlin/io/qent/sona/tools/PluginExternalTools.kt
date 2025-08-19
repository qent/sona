package io.qent.sona.tools

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.ModalityState
import com.intellij.util.Alarm
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiManager
import com.intellij.terminal.JBTerminalWidget
import com.intellij.terminal.ui.TerminalWidget
import io.qent.sona.Strings
import io.qent.sona.core.data.DirectoryListing
import io.qent.sona.core.data.FileDependenciesInfo
import io.qent.sona.core.data.FileLines
import io.qent.sona.core.data.FileStructureInfo
import io.qent.sona.core.tools.ExternalTools
import io.qent.sona.services.PatchService
import io.qent.sona.tools.dependencies.JavaFileDependenciesProvider
import io.qent.sona.tools.dependencies.KotlinFileDependenciesProvider
import io.qent.sona.tools.structure.*
import org.jetbrains.kotlin.psi.KtFile
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
    private var terminalWidget: TerminalWidget? = null

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
            FileStructureInfo(file.path, elements, document.lineCount)
        }
    }

    override fun getFileLines(path: String, fromLine: Int, toLine: Int): String? {
        return try {
            val p = Paths.get(path)
            val lines = Files.readAllLines(p)
            val start = fromLine.coerceAtLeast(1) - 1
            val end = toLine.coerceAtMost(lines.size)
            return if (start < end) lines.subList(start, end).joinToString("\n") else ""
        } catch (_: Exception) {
            null
        }
    }

    override fun applyPatch(chatId: String, patch: String): String {
        val result = if (ApplicationManager.getApplication().isDispatchThread) {
            project.service<PatchService>().applyPatch(patch)
        } else {
            var success = false
            ApplicationManager.getApplication().invokeAndWait {
                success = project.service<PatchService>().applyPatch(patch)
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

    override fun findFilesByNames(pattern: String, offset: Int, limit: Int): List<String> {
        TODO("Return list of files paths search results by pattern")
    }

    override fun findClasses(pattern: String, offset: Int, limit: Int): List<FileStructureInfo> {
        TODO("Return list of FileStructureInfo of classes founded by search by pattern")
    }

    override fun findText(pattern: String, offset: Int, limit: Int): Map<String, Map<Int, String>> {
        TODO("Return map of files path to lines numbers with lines content of search by pattern")
    }

    private fun getTerminal(): TerminalWidget {
        return if (ApplicationManager.getApplication().isDispatchThread) {
            getTerminalInternal()
        } else {
            var terminal: TerminalWidget? = null
            ApplicationManager.getApplication().invokeAndWait {
                terminal = getTerminalInternal()
            }
            terminal ?: throw IllegalStateException("Failed to create terminal widget")
        }
    }

    private fun getTerminalInternal(): TerminalWidget {
        val widget = terminalWidget
        if (widget != null && !Disposer.isDisposed(widget)) return widget
        val workingDir = project.basePath ?: System.getProperty("user.dir")
        val terminalManager = TerminalToolWindowManager.getInstance(project)
        val newWidget = terminalManager.createShellWidget(workingDir, "Sona", true, true)
        terminalWidget = newWidget
        return newWidget
    }

    override fun sendTerminalCommand(command: String): String {
        // Schedule command execution after the terminal widget is created & UI is ready
        ApplicationManager.getApplication().invokeLater({
            val widget = getTerminal()
            if (Disposer.isDisposed(widget)) return@invokeLater
            // Delay slightly to allow the shell session to initialize (prompt to appear)
            // Tie the alarm lifecycle to the widget to avoid leaks if the tab is closed
            Alarm(Alarm.ThreadToUse.SWING_THREAD, widget).addRequest({
                if (!Disposer.isDisposed(widget)) {
                    widget.sendCommandToExecute(command)
                }
            }, 100)
        }, ModalityState.NON_MODAL)
        return Strings.terminalCommandSent
    }

    override fun readTerminalOutput(): String {
        val widget = terminalWidget?.takeIf { !Disposer.isDisposed(it) }
            ?: TerminalToolWindowManager.getInstance(project)
                .terminalWidgets.firstOrNull { it.hasFocus() }
            ?: return ""

        if (widget.isCommandRunning()) {
            return "Strings.terminalCommandRunning"
        }

        return JBTerminalWidget.asJediTermWidget(widget)?.text.orEmpty()
    }
}
