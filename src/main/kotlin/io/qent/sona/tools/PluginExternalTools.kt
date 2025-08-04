package io.qent.sona.tools

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import io.qent.sona.core.ExternalTools

class PluginExternalTools(private val project: Project) : ExternalTools {
    override fun getFocusedFileText(): String? {
        return FileEditorManager.getInstance(project).selectedTextEditor?.document?.text
    }
}
