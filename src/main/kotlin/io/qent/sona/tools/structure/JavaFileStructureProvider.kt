package io.qent.sona.tools.structure

import com.intellij.openapi.editor.Document
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import io.qent.sona.core.data.FileElement
import io.qent.sona.core.data.FileElementType

class JavaFileStructureProvider : FileStructureProvider {
    override fun collect(psiFile: PsiFile, document: Document): List<FileElement> {
        val elements = mutableListOf<FileElement>()
        psiFile.accept(object : JavaRecursiveElementVisitor() {
            override fun visitClass(aClass: PsiClass) {
                addClass(aClass, document, elements)
                super.visitClass(aClass)
            }

            override fun visitMethod(method: PsiMethod) {
                addMethod(method, document, elements)
            }

            override fun visitField(field: PsiField) {
                addField(field, document, elements)
            }
        })
        return elements
    }

    private fun addClass(aClass: PsiClass, document: Document, elements: MutableList<FileElement>) {
        val name = aClass.name ?: return
        val type = when {
            aClass.isInterface -> FileElementType.INTERFACE
            aClass.isEnum -> FileElementType.ENUM
            else -> FileElementType.CLASS
        }
        val start = document.getLineNumber(aClass.textRange.startOffset) + 1
        val end = document.getLineNumber(aClass.textRange.endOffset) + 1
        val public = aClass.hasModifierProperty(PsiModifier.PUBLIC)
        elements.add(FileElement(name, type, public, start to end))
    }

    private fun addMethod(method: PsiMethod, document: Document, elements: MutableList<FileElement>) {
        val start = document.getLineNumber(method.textRange.startOffset) + 1
        val end = document.getLineNumber(method.textRange.endOffset) + 1
        val public = method.hasModifierProperty(PsiModifier.PUBLIC)
        elements.add(FileElement(method.name, FileElementType.METHOD, public, start to end))
    }

    private fun addField(field: PsiField, document: Document, elements: MutableList<FileElement>) {
        val start = document.getLineNumber(field.textRange.startOffset) + 1
        val end = document.getLineNumber(field.textRange.endOffset) + 1
        val public = field.hasModifierProperty(PsiModifier.PUBLIC)
        elements.add(FileElement(field.name, FileElementType.FIELD, public, start to end))
    }
}
