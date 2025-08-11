package io.qent.sona

import com.intellij.openapi.diagnostic.Logger
import io.qent.sona.core.Logger as CoreLogger

/**
 * Intellij-based logger implementation for the core logging facade.
 */
object IdeaLogger : CoreLogger {
    private val logger = Logger.getInstance("Sona")
    override fun log(message: String) {
        logger.info(message)
    }
}

