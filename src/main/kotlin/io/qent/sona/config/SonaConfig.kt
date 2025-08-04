package io.qent.sona.config

import com.google.gson.Gson
import java.io.File

class SonaConfig {
    var permissions: Permissions? = null

    class Permissions {
        var files: Files? = null

        class Files {
            var whitelist: List<String>? = null
            var blacklist: List<String>? = null
        }
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
