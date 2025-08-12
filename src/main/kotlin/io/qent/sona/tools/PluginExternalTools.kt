package io.qent.sona.tools

import com.intellij.openapi.components.service
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import io.qent.sona.Strings
import io.qent.sona.core.permissions.DirectoryListing
import io.qent.sona.core.permissions.FileInfo
import io.qent.sona.core.permissions.FileStructureInfo
import io.qent.sona.core.tools.ExternalTools
import io.qent.sona.services.PatchService
import io.qent.sona.tools.structure.JavaFileStructureProvider
import io.qent.sona.tools.structure.JavaScriptFileStructureProvider
import io.qent.sona.tools.structure.KotlinFileStructureProvider
import io.qent.sona.tools.structure.PythonFileStructureProvider
import io.qent.sona.tools.structure.TypeScriptFileStructureProvider
import org.jetbrains.kotlin.psi.KtFile
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList

class PluginExternalTools(private val project: Project) : ExternalTools {
    private val javaProvider = JavaFileStructureProvider()
    private val kotlinProvider = KotlinFileStructureProvider()
    private val pythonProvider = PythonFileStructureProvider()
    private val tsProvider = TypeScriptFileStructureProvider()
    private val jsProvider = JavaScriptFileStructureProvider()

    override fun getFocusedFileInfo(): FileStructureInfo? {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return null
        val file = editor.virtualFile ?: return null

        return runReadAction {
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

    override fun applyPatch(patch: String): String {
        project.service<PatchService>().applyPatch(patch)
        return Strings.patchDiffOpened
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
}
