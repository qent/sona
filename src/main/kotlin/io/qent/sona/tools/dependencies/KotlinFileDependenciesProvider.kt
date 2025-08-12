package io.qent.sona.tools.dependencies

import com.intellij.psi.PsiFile
import io.qent.sona.core.permissions.FileDependency
import org.jetbrains.kotlin.psi.KtFile

class KotlinFileDependenciesProvider : FileDependenciesProvider {
    override fun collect(psiFile: PsiFile): List<FileDependency> {
        val ktFile = psiFile as? KtFile ?: return emptyList()
        return ktFile.importDirectives.mapNotNull { directive ->
            val name = directive.importedFqName?.asString() ?: return@mapNotNull null
            val target = directive.importedReference?.references?.firstOrNull()?.resolve() ?: return@mapNotNull null
            val vFile = target.containingFile?.virtualFile ?: return@mapNotNull null
            FileDependency(name, vFile.path)
        }
    }
}
