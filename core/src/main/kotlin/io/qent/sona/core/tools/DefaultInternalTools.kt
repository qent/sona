package io.qent.sona.core.tools

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import io.qent.sona.core.roles.DefaultRoles

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
