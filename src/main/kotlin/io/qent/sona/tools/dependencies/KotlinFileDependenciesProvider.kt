package io.qent.sona.tools.dependencies

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import io.qent.sona.core.data.FileDependency
import org.jetbrains.kotlin.psi.*

class KotlinFileDependenciesProvider : FileDependenciesProvider {
    override fun collect(psiFile: PsiFile): List<FileDependency> {
        val ktFile = psiFile as? KtFile ?: return emptyList()

        val deps = mutableListOf<FileDependency>()
        val seen = mutableSetOf<String>()

        // Explicit imports
        ktFile.importDirectives.forEach { directive ->
            val name = directive.importedFqName?.asString() ?: return@forEach
            val target = directive.importedReference?.references?.firstOrNull()?.resolve() ?: return@forEach
            val vFile = target.containingFile?.virtualFile ?: return@forEach
            if (seen.add(name)) deps.add(FileDependency(name, vFile.path))
        }

        // Same-package references without imports
        val pkg = ktFile.packageFqName.asString()
        ktFile.accept(object : KtTreeVisitorVoid() {
            override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                super.visitSimpleNameExpression(expression)

                val target = expression.references.firstOrNull()?.resolve() ?: return
                val targetFile = target.containingFile ?: return
                if (targetFile == psiFile) return

                val fqName = when (target) {
                    is KtClassOrObject -> target.fqName?.asString()
                    is PsiClass -> target.qualifiedName
                    else -> null
                } ?: return

                val targetPkg = when (target) {
                    is KtClassOrObject -> target.containingKtFile.packageFqName.asString()
                    is PsiClass -> target.qualifiedName?.substringBeforeLast('.', "")
                    else -> null
                }

                if (targetPkg != pkg) return

                val vFile = targetFile.virtualFile ?: return
                if (seen.add(fqName)) deps.add(FileDependency(fqName, vFile.path))
            }
        })

        return deps
    }
}
