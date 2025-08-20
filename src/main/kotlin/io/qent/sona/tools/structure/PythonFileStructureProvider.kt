package io.qent.sona.tools.structure

import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import io.qent.sona.core.data.FileElement
import io.qent.sona.core.data.FileElementType

class PythonFileStructureProvider : FileStructureProvider {
    override fun collect(psiFile: PsiFile, document: Document): List<FileElement> {
        return try {
            val pyFileClass = Class.forName("com.jetbrains.python.psi.PyFile")
            if (!pyFileClass.isInstance(psiFile)) return emptyList()

            val pyClassClass = Class.forName("com.jetbrains.python.psi.PyClass") as Class<out PsiElement>
            val pyFunctionClass = Class.forName("com.jetbrains.python.psi.PyFunction") as Class<out PsiElement>
            val pyTargetClass = Class.forName("com.jetbrains.python.psi.PyTargetExpression") as Class<out PsiElement>
            val elements = mutableListOf<FileElement>()

            PsiTreeUtil.collectElementsOfType(psiFile, pyClassClass).forEach { elem ->
                val name = elem.javaClass.getMethod("getName").invoke(elem) as? String ?: return@forEach
                val start = document.getLineNumber(elem.textRange.startOffset) + 1
                val end = document.getLineNumber(elem.textRange.endOffset) + 1
                val public = !name.startsWith("_")
                elements.add(FileElement(name, FileElementType.CLASS, public, listOf(start, end)))
            }

            PsiTreeUtil.collectElementsOfType(psiFile, pyFunctionClass).forEach { elem ->
                val name = elem.javaClass.getMethod("getName").invoke(elem) as? String ?: return@forEach
                val start = document.getLineNumber(elem.textRange.startOffset) + 1
                val end = document.getLineNumber(elem.textRange.endOffset) + 1
                val public = !name.startsWith("_")
                elements.add(FileElement(name, FileElementType.METHOD, public, listOf(start, end)))
            }

            PsiTreeUtil.collectElementsOfType(psiFile, pyTargetClass).forEach { elem ->
                val name = elem.javaClass.getMethod("getName").invoke(elem) as? String ?: return@forEach
                val start = document.getLineNumber(elem.textRange.startOffset) + 1
                val end = document.getLineNumber(elem.textRange.endOffset) + 1
                val public = !name.startsWith("_")
                elements.add(FileElement(name, FileElementType.FIELD, public, listOf(start, end)))
            }

            elements
        } catch (_: ClassNotFoundException) {
            emptyList()
        }
    }
}
