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
        val languages = detectLanguages(base)
        val extensions = detectExtensionStats(base)
        val builds = detectBuildSystems(base)
        return listOf(
            "OS: $os",
            "IDE: $ide",
            "Java: $java",
            "Python: $python",
            "Node: $node",
            "Languages: $languages",
            "Extensions: $extensions",
            "Builds: $builds",
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
        val systems = mutableListOf<String>()
        fun has(vararg names: String) = names.any { File(base, it).exists() }
        if (has("build.gradle.kts", "build.gradle")) systems.add("Gradle")
        if (has("pom.xml")) systems.add("Maven")
        if (has("build.xml")) systems.add("Ant")
        if (has("build.sbt")) systems.add("SBT")
        if (has("CMakeLists.txt")) systems.add("CMake")
        if (has("Makefile")) systems.add("Make")
        if (has("BUILD", "BUILD.bazel", "WORKSPACE")) systems.add("Bazel")
        if (has("package.json")) systems.add("npm")
        if (has("yarn.lock")) systems.add("Yarn")
        if (has("pnpm-lock.yaml")) systems.add("pnpm")
        if (has("requirements.txt")) systems.add("Pip")
        if (has("pyproject.toml")) systems.add("Poetry")
        if (has("Pipfile")) systems.add("Pipenv")
        if (has("Cargo.toml")) systems.add("Cargo")
        if (has("go.mod")) systems.add("Go")
        if (has("composer.json")) systems.add("Composer")
        return if (systems.isEmpty()) "Unknown" else systems.joinToString(", ")
    }

    private fun detectLanguages(base: File?): String {
        if (base == null) return "Unknown"
        val extToLang = mapOf(
            "kt" to "Kotlin",
            "kts" to "Kotlin",
            "java" to "Java",
            "scala" to "Scala",
            "groovy" to "Groovy",
            "py" to "Python",
            "js" to "JavaScript",
            "jsx" to "JavaScript",
            "ts" to "TypeScript",
            "tsx" to "TypeScript",
            "go" to "Go",
            "rs" to "Rust",
            "php" to "PHP",
            "rb" to "Ruby",
            "swift" to "Swift",
            "m" to "Objective-C",
            "mm" to "Objective-C++",
            "c" to "C",
            "h" to "C/C++",
            "cpp" to "C++",
            "cxx" to "C++",
            "cc" to "C++",
            "hpp" to "C++",
            "hh" to "C++",
            "cs" to "C#",
            "fs" to "F#",
            "html" to "HTML",
            "htm" to "HTML",
            "css" to "CSS",
            "scss" to "SCSS",
            "less" to "LESS",
            "xml" to "XML",
            "json" to "JSON",
            "yaml" to "YAML",
            "yml" to "YAML",
            "sh" to "Shell",
            "bash" to "Shell",
            "sql" to "SQL",
            "dart" to "Dart",
        )
        val languages = mutableSetOf<String>()
        base.walkTopDown().maxDepth(4).forEach { f ->
            if (f.isFile) {
                extToLang[f.extension.lowercase()]?.let { languages.add(it) }
            }
        }
        return if (languages.isEmpty()) "Unknown" else languages.sorted().joinToString(", ")
    }

    private fun detectExtensionStats(base: File?): String {
        if (base == null) return "Unknown"
        val counts = mutableMapOf<String, Int>()
        base.walkTopDown().maxDepth(4).forEach { f ->
            if (f.isFile) {
                val ext = f.extension.lowercase()
                if (ext.isNotEmpty()) {
                    counts[ext] = (counts[ext] ?: 0) + 1
                }
            }
        }
        if (counts.isEmpty()) return "Unknown"
        return counts.entries
            .sortedByDescending { it.value }
            .joinToString(", ") { "${it.key}: ${it.value}" }
    }

    override suspend fun collect(collector: FlowCollector<State>) {
        stateProvider.state.collect(collector)
    }
}