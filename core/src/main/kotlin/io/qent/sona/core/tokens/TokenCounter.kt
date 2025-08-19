package io.qent.sona.core.tokens

import io.qent.sona.core.presets.Preset

/** Counts tokens in text for a given model preset. */
interface TokenCounter {
    /** Return number of tokens contained in [text] for [preset]. */
    suspend fun count(text: String, preset: Preset): Int
}
