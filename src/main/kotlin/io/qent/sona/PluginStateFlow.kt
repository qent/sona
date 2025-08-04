package io.qent.sona

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.application.ApplicationInfo
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel
import dev.langchain4j.model.openai.OpenAiStreamingChatModel
import dev.langchain4j.data.message.SystemMessage
import io.qent.sona.core.*
import io.qent.sona.tools.PluginExternalTools
import io.qent.sona.repositories.PluginChatRepository
import io.qent.sona.repositories.PluginPresetsRepository
import io.qent.sona.repositories.PluginRolesRepository
import io.qent.sona.repositories.PluginSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*


@Service(Service.Level.PROJECT)
class PluginStateFlow(private val project: Project) : Flow<State> {

    private val settingsRepository = service<PluginSettingsRepository>()
    private val presetsRepository = service<PluginPresetsRepository>()
    private val chatRepository = service<PluginChatRepository>()
    private val rolesRepository = service<PluginRolesRepository>()
    private val scope = CoroutineScope(Dispatchers.Default)
    private var stateProvider: StateProvider

    private val externalTools = PluginExternalTools(project)

    var lastState: State = State.ChatState(
        messages = emptyList(),
        outputTokens = 0,
        inputTokens = 0,
        isSending = false,
        roles = emptyList(),
        activeRole = 0,
        onSelectRole = {},
        presets = Presets(-1, emptyList()),
        onSelectPreset = {},
        onSendMessage = {},
        onStop = {},
        onDeleteFrom = {},
        toolRequest = false,
        onAllowTool = {},
        onAlwaysAllowTool = {},
        onDenyTool = {},
        onNewChat = {},
        onOpenHistory = {},
        onOpenRoles = {},
        onOpenPresets = {},
    )

    init {
        stateProvider = StateProvider(
            presetsRepository,
            chatRepository,
            rolesRepository,
            modelFactory = { preset ->
                when (preset.provider) {
                    LlmProvider.Anthropic -> AnthropicStreamingChatModel.builder()
                        .apiKey(preset.apiKey)
                        .baseUrl(preset.apiEndpoint)
                        .modelName(preset.model)
                        .build()

                    LlmProvider.OpenAI -> OpenAiStreamingChatModel.builder()
                        .apiKey(preset.apiKey)
                        .baseUrl(preset.apiEndpoint)
                        .modelName(preset.model)
                        .build()

                    LlmProvider.Deepseek -> OpenAiStreamingChatModel.builder()
                        .apiKey(preset.apiKey)
                        .baseUrl(preset.apiEndpoint)
                        .modelName(preset.model)
                        .build()

                    LlmProvider.Gemini -> GoogleAiGeminiStreamingChatModel.builder()
                        .apiKey(preset.apiKey)
                        .baseUrl(preset.apiEndpoint)
                        .modelName(preset.model)
                        .build()
                }
            },
            externalTools = externalTools,
            scope = scope,
            systemMessages = createSystemMessages(),
        )

        stateProvider.state.onEach {
            lastState = it
        }.launchIn(scope)

        scope.launch {
            if (settingsRepository.load().ignoreHttpsErrors) {
                val trustAll: Array<TrustManager> = arrayOf(object : X509TrustManager {
                    override fun checkClientTrusted(c: Array<X509Certificate?>?, a: String?) = Unit
                    override fun checkServerTrusted(c: Array<X509Certificate?>?, a: String?) = Unit
                    override fun getAcceptedIssuers(): Array<X509Certificate?>? = arrayOfNulls(0)
                })
                val sc = SSLContext.getInstance("SSL")
                sc.init(null, trustAll, SecureRandom())
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
                HttpsURLConnection.setDefaultHostnameVerifier { _: String?, _: SSLSession? -> true }
            }
        }
    }

    private fun createSystemMessages(): List<SystemMessage> {
        return listOf(SystemMessage.from(environmentInfo()))
    }

    private fun environmentInfo(): String {
        val os = "${System.getProperty("os.name")} ${System.getProperty("os.version")}".trim()
        val ide = ApplicationInfo.getInstance().fullApplicationName
        val java = System.getProperty("java.version")
        val python = runCommand("python", "--version")
        val node = runCommand("node", "--version")
        val base = project.basePath?.let { File(it) }
        val extCounts = collectExtensionStats(base)
        val language = detectLanguages(extCounts)
        val extensions = formatExtensionStats(extCounts)
        val builds = detectBuildSystems(base)
        return listOf(
            "OS: $os",
            "IDE: $ide",
            "Java: $java",
            "Python: $python",
            "Node: $node",
            "Languages: $language",
            "Extensions: $extensions",
            "Build systems: $builds",
        ).joinToString("\n")
    }

    private fun runCommand(vararg cmd: String): String {
        return runCatching {
            val proc = ProcessBuilder(*cmd).redirectErrorStream(true).start()
            proc.inputStream.bufferedReader().use { it.readText() }.trim()
        }.getOrElse { "Unknown" }
    }

    private fun detectBuildSystems(base: File?): String {
        if (base == null) return "Unknown"
        val systems = mutableSetOf<String>()
        base.walkTopDown().maxDepth(2).forEach { f ->
            if (f.isFile) {
                when (f.name.lowercase()) {
                    "build.gradle.kts", "build.gradle" -> systems.add("Gradle")
                    "pom.xml" -> systems.add("Maven")
                    "build.xml" -> systems.add("Ant")
                    "build.sbt" -> systems.add("sbt")
                    "package.json" -> systems.add("npm")
                    "yarn.lock" -> systems.add("Yarn")
                    "pnpm-lock.yaml", "pnpm-lock.yml" -> systems.add("pnpm")
                    "requirements.txt" -> systems.add("pip")
                    "pyproject.toml" -> systems.add("Poetry")
                    "cmakelists.txt" -> systems.add("CMake")
                    "makefile" -> systems.add("Make")
                    "cargo.toml" -> systems.add("Cargo")
                }
            }
        }
        return if (systems.isEmpty()) "Unknown" else systems.sorted().joinToString(", ")
    }

    private val extensionLanguages = mapOf(
        "kt" to "Kotlin",
        "kts" to "Kotlin",
        "java" to "Java",
        "groovy" to "Groovy",
        "scala" to "Scala",
        "clj" to "Clojure",
        "py" to "Python",
        "rb" to "Ruby",
        "php" to "PHP",
        "go" to "Go",
        "rs" to "Rust",
        "swift" to "Swift",
        "m" to "Objective-C",
        "mm" to "Objective-C++",
        "cs" to "C#",
        "fs" to "F#",
        "c" to "C",
        "cc" to "C++",
        "cpp" to "C++",
        "cxx" to "C++",
        "h" to "C/C++",
        "hpp" to "C++",
        "hxx" to "C++",
        "js" to "JavaScript",
        "jsx" to "JavaScript",
        "ts" to "TypeScript",
        "tsx" to "TypeScript",
        "html" to "HTML",
        "htm" to "HTML",
        "css" to "CSS",
        "scss" to "SCSS",
        "less" to "Less",
        "xml" to "XML",
        "json" to "JSON",
        "yml" to "YAML",
        "yaml" to "YAML",
        "md" to "Markdown",
        "dart" to "Dart",
    )

    private fun collectExtensionStats(base: File?): Map<String, Int> {
        if (base == null) return emptyMap()
        val counts = mutableMapOf<String, Int>()
        base.walkTopDown().maxDepth(4).forEach { f ->
            if (f.isFile) {
                val ext = f.extension.lowercase()
                if (extensionLanguages.containsKey(ext)) {
                    counts[ext] = counts.getOrDefault(ext, 0) + 1
                }
            }
        }
        return counts
    }

    private fun detectLanguages(counts: Map<String, Int>): String {
        if (counts.isEmpty()) return "Unknown"
        val languages = counts.keys.mapNotNull { extensionLanguages[it] }.toSet().sorted()
        return languages.joinToString(", ")
    }

    private fun formatExtensionStats(counts: Map<String, Int>): String {
        if (counts.isEmpty()) return "Unknown"
        return counts.entries.sortedByDescending { it.value }
            .joinToString(", ") { "${it.key}(${it.value})" }
    }

    override suspend fun collect(collector: FlowCollector<State>) {
        stateProvider.state.collect(collector)
    }
}