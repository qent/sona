package io.qent.sona.tools.dependencies

import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import io.qent.sona.core.data.FileDependency

class JavaFileDependenciesProvider : FileDependenciesProvider {
    override fun collect(psiFile: PsiFile): List<FileDependency> {
        val javaFile = psiFile as? PsiJavaFile ?: return emptyList()
        val imports = javaFile.importList?.allImportStatements ?: return emptyList()
        return imports.mapNotNull { stmt ->
            val ref = stmt.importReference?.resolve() ?: return@mapNotNull null
            val vFile = ref.containingFile?.virtualFile ?: return@mapNotNull null
            val name = stmt.importReference?.qualifiedName ?: return@mapNotNull null
            FileDependency(name, vFile.path)
        }
    }
}
