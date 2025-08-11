package io.qent.sona.core

/**
 * Simple logging facade used by the core module so the logic stays free of
 * any IntelliJ SDK dependencies. The actual logging implementation is
 * provided by the consumer of the core module.
 */
interface Logger {
    fun log(message: String)

    object NoOp : Logger {
        override fun log(message: String) {}
    }
}

