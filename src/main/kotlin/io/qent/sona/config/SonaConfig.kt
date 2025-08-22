package io.qent.sona.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import java.io.File

class SonaConfig {
    var permissions: Permissions? = null
    var mcpServers: MutableMap<String, McpServer>? = null

    class Permissions {
        var files: Files? = null

        class Files {
            var whitelist: List<String>? = null
            var blacklist: List<String>? = null
        }
    }

    class McpServer {
        var enabled: Boolean? = null
        var command: String? = null
        var args: List<String>? = null
        var env: Map<String, String>? = null
        var transport: String? = "stdio"
        var url: String? = null
        var cwd: String? = null
        var headers: Map<String, String>? = null
        var disabledTools: List<String>? = null
    }

    companion object {
        private fun file(root: String, role: String? = null): File {
            return if (role != null) {
                val agentsDir = File(File(root, ".sona"), "agents")
                val roleFile = File(File(agentsDir, role.lowercase()), "sona.json")
                if (roleFile.exists()) roleFile else File(File(root, ".sona"), "sona.json")
            } else {
                File(File(root, ".sona"), "sona.json")
            }
        }

        fun load(root: String, role: String? = null): SonaConfig? {
            val file = file(root, role)
            return runCatching {
                if (file.exists()) file.reader().use { Gson().fromJson(it, SonaConfig::class.java) } else null
            }.getOrNull()
        }

        fun save(root: String, config: SonaConfig, role: String? = null) {
            val file = file(root, role)
            file.parentFile?.mkdirs()
            val gson: Gson = GsonBuilder().setPrettyPrinting().create()

            val newJson = gson.toJsonTree(config).asJsonObject
            val merged = if (file.exists()) {
                file.reader().use { reader ->
                    val existing = Gson().fromJson(reader, JsonObject::class.java) ?: JsonObject()
                    existing.deepMerge(newJson)
                }
            } else {
                newJson
            }

            file.writer().use { gson.toJson(merged, it) }
        }

        fun path(root: String, role: String? = null): File = file(root, role)

        private fun JsonObject.deepMerge(other: JsonObject): JsonObject {
            other.entrySet().forEach { (key, value) ->
                if (this[key]?.isJsonObject == true && value.isJsonObject) {
                    this[key].asJsonObject.deepMerge(value.asJsonObject)
                } else {
                    this.add(key, value)
                }
            }
            return this
        }
    }
}
