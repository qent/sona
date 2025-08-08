package io.qent.sona.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PatchUtilsTest {
    @Test
    fun detectsPatchByLanguage() {
        val code = "random text"
        assertTrue(isPatch(code, "diff"))
        assertTrue(isPatch(code, "patch"))
        assertTrue(isPatch(code, "udiff"))
    }

    @Test
    fun detectsPatchByContent() {
        val patch = """
            --- a/file.txt
            +++ b/file.txt
            @@
            -old
            +new
        """.trimIndent()
        assertTrue(isPatch(patch))
    }

    @Test
    fun rejectsNonPatchContent() {
        val code = "fun main() {\n println(\"hi\")\n}"
        assertFalse(isPatch(code))
        assertFalse(isPatch(code, "kotlin"))
    }

    @Test
    fun rejectsIncompletePatchMarkers() {
        val code = """
            --- a/file.txt
            @@
            -old
            +new
        """.trimIndent()
        assertFalse(isPatch(code))
    }
}
