package io.qent.sona.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DefaultInternalTools(
    private val scope: CoroutineScope,
    private val selectRole: suspend (DefaultRoles) -> Unit,
) : InternalTools {

    override fun switchToArchitect(): String {
        scope.launch { selectRole(DefaultRoles.ARCHITECT) }
        return "Architect mode active"
    }

    override fun switchToCode(): String {
        scope.launch { selectRole(DefaultRoles.CODE) }
        return "Code mode active"
    }
}
