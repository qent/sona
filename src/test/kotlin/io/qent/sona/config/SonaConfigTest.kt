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
        val file = File(dir, "sona.json")
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
}
