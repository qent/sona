package io.qent.sona.tools.structure

import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import io.qent.sona.core.data.FileElement
import io.qent.sona.core.data.FileElementType

abstract class AbstractJsFileStructureProvider : FileStructureProvider {
    override fun collect(psiFile: PsiFile, document: Document): List<FileElement> {
        return try {
            val jsFileClass = Class.forName("com.intellij.lang.javascript.psi.JSFile")
            if (!jsFileClass.isInstance(psiFile)) return emptyList()

            val jsClassClass = Class.forName("com.intellij.lang.javascript.psi.JSClass") as Class<out PsiElement>
            val jsFunctionClass = Class.forName("com.intellij.lang.javascript.psi.JSFunction") as Class<out PsiElement>
            val jsFieldClass = Class.forName("com.intellij.lang.javascript.psi.JSField") as Class<out PsiElement>
            val attrListClass = Class.forName("com.intellij.lang.javascript.psi.JSAttributeList")
            val accessTypeClass = Class.forName("com.intellij.lang.javascript.psi.ecma6.impl.JSAttributeListAccessType") as Class<out Enum<*>>
            val getAccessType = attrListClass.getMethod("getAccessType")
            val privateConst = java.lang.Enum.valueOf(accessTypeClass, "PRIVATE")
            val protectedConst = java.lang.Enum.valueOf(accessTypeClass, "PROTECTED")

            fun isPublic(element: Any): Boolean {
                val attr = element.javaClass.getMethod("getAttributeList").invoke(element) ?: return true
                val access = getAccessType.invoke(attr)
                return access != privateConst && access != protectedConst
            }

            val elements = mutableListOf<FileElement>()

            PsiTreeUtil.collectElementsOfType(psiFile, jsClassClass).forEach { elem ->
                val name = elem.javaClass.getMethod("getName").invoke(elem) as? String ?: return@forEach
                val start = document.getLineNumber(elem.textRange.startOffset) + 1
                val end = document.getLineNumber(elem.textRange.endOffset) + 1
                elements.add(FileElement(name, FileElementType.CLASS, isPublic(elem), listOf(start, end)))
            }

            PsiTreeUtil.collectElementsOfType(psiFile, jsFunctionClass).forEach { elem ->
                val name = elem.javaClass.getMethod("getName").invoke(elem) as? String ?: return@forEach
                val start = document.getLineNumber(elem.textRange.startOffset) + 1
                val end = document.getLineNumber(elem.textRange.endOffset) + 1
                elements.add(FileElement(name, FileElementType.METHOD, isPublic(elem), listOf(start, end)))
            }

            PsiTreeUtil.collectElementsOfType(psiFile, jsFieldClass).forEach { elem ->
                val name = elem.javaClass.getMethod("getName").invoke(elem) as? String ?: return@forEach
                val start = document.getLineNumber(elem.textRange.startOffset) + 1
                val end = document.getLineNumber(elem.textRange.endOffset) + 1
                elements.add(FileElement(name, FileElementType.FIELD, isPublic(elem), listOf(start, end)))
            }

            elements
        } catch (_: ClassNotFoundException) {
            emptyList()
        }
    }
}

class JavaScriptFileStructureProvider : AbstractJsFileStructureProvider()

class TypeScriptFileStructureProvider : AbstractJsFileStructureProvider()
