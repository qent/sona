package io.qent.sona.tools.structure

import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiFile
import io.qent.sona.core.data.FileElement
import io.qent.sona.core.data.FileElementType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

class KotlinFileStructureProvider : FileStructureProvider {
    override fun collect(psiFile: PsiFile, document: Document): List<FileElement> {
        val ktFile = psiFile as? KtFile ?: return emptyList()
        val elements = mutableListOf<FileElement>()
        ktFile.declarations.forEach { collectKt(it, document, elements) }
        return elements
    }

    private fun collectKt(decl: KtDeclaration, document: Document, elements: MutableList<FileElement>) {
        val name = decl.name ?: return
        val type = when (decl) {
            is KtClass -> when {
                decl.isInterface() -> FileElementType.INTERFACE
                decl.isEnum() -> FileElementType.ENUM
                else -> FileElementType.CLASS
            }
            is KtObjectDeclaration -> FileElementType.OBJECT
            is KtNamedFunction -> FileElementType.METHOD
            is KtProperty -> FileElementType.FIELD
            else -> null
        } ?: return
        val start = document.getLineNumber(decl.textRange.startOffset) + 1
        val end = document.getLineNumber(decl.textRange.endOffset) + 1
        val modifiers = decl.modifierList
        val public = (modifiers?.hasModifier(KtTokens.PUBLIC_KEYWORD) ?: true) &&
            !(modifiers?.hasModifier(KtTokens.PRIVATE_KEYWORD) == true ||
                    modifiers?.hasModifier(KtTokens.PROTECTED_KEYWORD) == true ||
                    modifiers?.hasModifier(KtTokens.INTERNAL_KEYWORD) == true)
        elements.add(FileElement(name, type, public, listOf(start, end)))
        if (decl is KtClassOrObject) {
            decl.declarations.forEach { collectKt(it, document, elements) }
        }
    }
}
