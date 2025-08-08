package io.qent.sona.util

fun isPatch(text: String, lang: String? = null): Boolean {
    val l = lang?.lowercase()
    return when {
        l != null && (l == "diff" || l == "patch" || l == "udiff") -> true
        else -> {
            val lines = text.lines()
            lines.size >= 2 && lines[0].startsWith("---") && lines[1].startsWith("+++")
        }
    }
}
