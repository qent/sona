package io.qent.sona

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.application.ApplicationInfo
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel
import dev.langchain4j.model.openai.OpenAiStreamingChatModel
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.http.client.jdk.JdkHttpClient
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder
import io.qent.sona.core.*
import io.qent.sona.tools.PluginExternalTools
import io.qent.sona.repositories.PluginChatRepository
import io.qent.sona.repositories.PluginPresetsRepository
import io.qent.sona.repositories.PluginRolesRepository
import io.qent.sona.repositories.PluginSettingsRepository
import io.qent.sona.repositories.PluginFilePermissionsRepository
import io.qent.sona.repositories.PluginMcpServersRepository
import io.qent.sona.core.FilePermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.File
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.net.http.HttpClient
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
        totalTokenUsage = TokenUsageInfo(),
        lastTokenUsage = TokenUsageInfo(),
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
        autoApproveTools = false,
        onToggleAutoApprove = {},
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
                    LlmProvider.Anthropic -> {
                        val builder = AnthropicStreamingChatModel.builder()
                            .apiKey(preset.apiKey)
                            .baseUrl(preset.apiEndpoint)
                            .modelName(preset.model)
                        if (settingsRepository.state.ignoreHttpsErrors) {
                            builder.httpClientBuilder(ignoreHttpsClientBuilder())
                        }
                        builder.build()
                    }

                    LlmProvider.OpenAI -> {
                        val builder = OpenAiStreamingChatModel.builder()
                            .apiKey(preset.apiKey)
                            .baseUrl(preset.apiEndpoint)
                            .modelName(preset.model)
                        if (settingsRepository.state.ignoreHttpsErrors) {
                            builder.httpClientBuilder(ignoreHttpsClientBuilder())
                        }
                        builder.build()
                    }

                    LlmProvider.Deepseek -> {
                        val builder = OpenAiStreamingChatModel.builder()
                            .apiKey(preset.apiKey)
                            .baseUrl(preset.apiEndpoint)
                            .modelName(preset.model)
                        if (settingsRepository.state.ignoreHttpsErrors) {
                            builder.httpClientBuilder(ignoreHttpsClientBuilder())
                        }
                        builder.build()
                    }

                    LlmProvider.Gemini -> {
                        val builder = GoogleAiGeminiStreamingChatModel.builder()
                            .apiKey(preset.apiKey)
                            .baseUrl(preset.apiEndpoint)
                            .modelName(preset.model)
                        if (settingsRepository.state.ignoreHttpsErrors) {
                            builder.httpClientBuilder(ignoreHttpsClientBuilder())
                        }
                        builder.build()
                    }
                }
            },
            externalTools = externalTools,
            filePermissionRepository = PluginFilePermissionsRepository(project),
            mcpServersRepository = PluginMcpServersRepository(project),
            scope = scope,
            systemMessages = createSystemMessages(),
        )

        stateProvider.state.onEach {
            lastState = it
        }.launchIn(scope)
    }

    private fun ignoreHttpsClientBuilder(): JdkHttpClientBuilder {
        val trustAll: Array<TrustManager> = arrayOf(object : X509TrustManager {
            override fun checkClientTrusted(certs: Array<X509Certificate?>?, authType: String?) = Unit
            override fun checkServerTrusted(certs: Array<X509Certificate?>?, authType: String?) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate?>? = arrayOfNulls(0)
        })
        val sslContext = SSLContext.getInstance("SSL").apply {
            init(null, trustAll, SecureRandom())
        }
        val httpClientBuilder = HttpClient.newBuilder().sslContext(sslContext)
        return JdkHttpClient.builder().httpClientBuilder(httpClientBuilder)
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
        val extensions = collectExtensionStats(base)
        val builds = detectBuildSystems(base)
        return listOf(
            "OS: $os",
            "IDE: $ide",
            "Java: $java",
            "Python: $python",
            "Node: $node",
            "Extensions: $extensions",
            "Build: $builds",
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
        fun exists(vararg names: String) = names.any { File(base, it).exists() }
        if (exists("build.gradle.kts", "build.gradle")) systems.add("Gradle")
        if (exists("pom.xml")) systems.add("Maven")
        if (exists("build.sbt")) systems.add("SBT")
        if (exists("build.xml")) systems.add("Ant")
        if (exists("CMakeLists.txt")) systems.add("CMake")
        if (exists("Makefile")) systems.add("Make")
        if (exists("BUILD", "BUILD.bazel", "WORKSPACE")) systems.add("Bazel")
        if (exists("package.json")) systems.add("npm")
        if (exists("yarn.lock")) systems.add("Yarn")
        if (exists("pnpm-lock.yaml")) systems.add("pnpm")
        if (exists("requirements.txt")) systems.add("pip")
        if (exists("pyproject.toml")) systems.add("Poetry")
        if (exists("go.mod")) systems.add("Go Modules")
        if (exists("Cargo.toml")) systems.add("Cargo")
        if (exists("composer.json")) systems.add("Composer")
        if (exists("mix.exs")) systems.add("Mix")
        if (exists("Package.swift")) systems.add("SwiftPM")
        if (exists("pubspec.yaml")) systems.add("Flutter")
        return if (systems.isEmpty()) "Unknown" else systems.sorted().joinToString(", ")
    }

    private fun collectExtensionStats(base: File?): String {
        if (base == null) return "Unknown"
        val allowed = setOf(
            "kt", "kts", "java", "py", "js", "ts", "jsx", "tsx", "rb", "go", "rs",
            "c", "h", "cpp", "cc", "cxx", "hpp", "hh", "hxx", "cs", "php", "swift",
            "dart", "scala", "groovy", "m", "mm", "sh", "ps1", "bat", "json", "yaml",
            "yml", "xml", "html", "css", "scss", "less", "sql", "md"
        )
        val counts = mutableMapOf<String, Int>()
        base.walkTopDown().maxDepth(4).forEach { f ->
            if (f.isFile) {
                val ext = f.extension.lowercase()
                if (ext in allowed) counts[ext] = counts.getOrDefault(ext, 0) + 1
            }
        }
        if (counts.isEmpty()) return "Unknown"
        return counts.entries
            .sortedByDescending { it.value }
            .joinToString(", ") { "${it.key}:${it.value}" }
    }

    override suspend fun collect(collector: FlowCollector<State>) {
        stateProvider.state.collect(collector)
    }
}