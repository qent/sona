package io.qent.sona.prompts

import io.qent.sona.loadProjectPromptMessages
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files

class ProjectPromptsTest {
    @Test
    fun `loads markdown files from user directory`() {
        val dir = Files.createTempDirectory("sonaPromptsTest")
        val promptsDir = dir.resolve(".sona").resolve("prompts")
        Files.createDirectories(promptsDir)
        Files.writeString(promptsDir.resolve("a.md"), "one")
        Files.writeString(promptsDir.resolve("b.md"), "two")
        Files.writeString(promptsDir.resolve("c.txt"), "ignored")

        val messages = loadProjectPromptMessages(dir.toString(), "Architect")
        assertEquals(2, messages.size)
    }

    @Test
    fun `loads role specific markdown files`() {
        val dir = Files.createTempDirectory("sonaPromptsTest")
        val promptsDir = dir.resolve(".sona").resolve("prompts")
        Files.createDirectories(promptsDir)
        Files.writeString(promptsDir.resolve("a.md"), "one")
        val roleDir = dir.resolve(".sona").resolve("agents").resolve("architect").resolve("prompts")
        Files.createDirectories(roleDir)
        Files.writeString(roleDir.resolve("b.md"), "two")

        val messages = loadProjectPromptMessages(dir.toString(), "Architect")
        assertEquals(2, messages.size)

        val other = loadProjectPromptMessages(dir.toString(), "Code")
        assertEquals(1, other.size)
    }
}
