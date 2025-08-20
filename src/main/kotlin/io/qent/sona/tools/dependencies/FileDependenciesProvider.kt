package io.qent.sona.tools.dependencies

import com.intellij.psi.PsiFile
import io.qent.sona.core.data.FileDependency

interface FileDependenciesProvider {
    fun collect(psiFile: PsiFile): List<FileDependency>
}
