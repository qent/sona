package io.qent.sona.config

import com.google.gson.Gson
import java.io.File

class SonaConfig {
    var permissions: Permissions? = null
    var mcpServers: List<McpServer>? = null

    class Permissions {
        var files: Files? = null

        class Files {
            var whitelist: List<String>? = null
            var blacklist: List<String>? = null
        }
    }

    class McpServer {
        var name: String? = null
        var command: String? = null
        var args: List<String>? = null
        var env: Map<String, String>? = null
        var transport: String? = null
        var url: String? = null
        var cwd: String? = null
        var headers: Map<String, String>? = null
    }

    companion object {
        fun load(root: String): SonaConfig? {
            val file = File(root, "sona.json")
            return runCatching {
                if (file.exists()) file.reader().use { Gson().fromJson(it, SonaConfig::class.java) } else null
            }.getOrNull()
        }
    }
}
