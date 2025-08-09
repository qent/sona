package io.qent.sona.core.tools

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DefaultInternalTools(
    private val scope: CoroutineScope,
    private val selectRole: suspend (String) -> Unit,
) : InternalTools {

    override fun switchRole(name: String): String {
        scope.launch { selectRole(name) }
        return "$name role active"
    }
}
