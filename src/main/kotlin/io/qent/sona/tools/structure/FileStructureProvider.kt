package io.qent.sona.tools.structure

import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiFile
import io.qent.sona.core.permissions.FileElement

interface FileStructureProvider {
    fun collect(psiFile: PsiFile, document: Document): List<FileElement>
}
