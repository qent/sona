package io.qent.sona.tools

import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import io.qent.sona.Strings
import io.qent.sona.core.permissions.FileInfo
import io.qent.sona.core.tools.ExternalTools
import io.qent.sona.services.PatchService
import java.nio.file.Files
import java.nio.file.Paths

class PluginExternalTools(private val project: Project) : ExternalTools {
    override fun getFocusedFileText(): FileInfo? {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return null
        val file = editor.virtualFile ?: return null
        return FileInfo(file.path, editor.document.text)
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
        return Strings.patchDialogOpened
    }
}
