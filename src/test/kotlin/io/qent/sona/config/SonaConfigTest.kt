package io.qent.sona.config

import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SonaConfigTest {
    @Test
    fun `saving merges with existing json`() {
        val dir = Files.createTempDirectory("sonaTest").toFile()
        val file = File(dir, ".sona/sona.json")
        file.parentFile.mkdirs()
        file.writeText("{\"foo\":\"bar\",\"permissions\":{\"files\":{\"whitelist\":[\"a\"]}}}")

        val config = SonaConfig()
        config.mcpServers = mutableMapOf("srv" to SonaConfig.McpServer().apply { enabled = true })

        SonaConfig.save(dir.absolutePath, config)

        val result = file.readText()
        val json = Gson().fromJson(result, JsonObject::class.java)
        assertEquals("bar", json.get("foo").asString)
        assertTrue(json.getAsJsonObject("mcpServers").has("srv"))
        assertTrue(json.getAsJsonObject("permissions").isJsonObject)
    }

    @Test
    fun `role config overrides main config`() {
        val dir = Files.createTempDirectory("sonaRoleTest").toFile()
        val main = File(dir, ".sona/sona.json")
        main.parentFile.mkdirs()
        main.writeText("{\"permissions\":{\"files\":{\"whitelist\":[\"main\"]}}}")

        val roleDir = File(dir, ".sona/agents/architect")
        roleDir.mkdirs()
        File(roleDir, "sona.json").writeText("{\"permissions\":{\"files\":{\"whitelist\":[\"role\"]}}}")

        val config = SonaConfig.load(dir.absolutePath, "Architect")
        assertEquals(listOf("role"), config?.permissions?.files?.whitelist)
    }

    @Test
    fun `save writes to role config when present`() {
        val dir = Files.createTempDirectory("sonaRoleSave").toFile()
        val roleDir = File(dir, ".sona/agents/architect")
        roleDir.mkdirs()
        val roleFile = File(roleDir, "sona.json")
        roleFile.writeText("{}")

        val config = SonaConfig()
        config.mcpServers = mutableMapOf("srv" to SonaConfig.McpServer().apply { enabled = true })

        SonaConfig.save(dir.absolutePath, config, "Architect")

        assertTrue(roleFile.exists())
    }
}
