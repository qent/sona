package io.qent.sona.tools.dependencies

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.psi.KtFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class KotlinFileDependenciesProviderTest : BasePlatformTestCase() {
    fun `test detects same package dependency`() {
        myFixture.addFileToProject(
            "src/com/example/Other.kt",
            "package com.example\nclass Other"
        )
        val file = myFixture.addFileToProject(
            "src/com/example/Use.kt",
            "package com.example\nclass Use { val o = Other() }"
        ) as KtFile

        val deps = KotlinFileDependenciesProvider().collect(file)
        assertEquals(1, deps.size)
        val dep = deps.first()
        assertEquals("com.example.Other", dep.name)
        assertTrue(dep.path.endsWith("Other.kt"))
    }
}
