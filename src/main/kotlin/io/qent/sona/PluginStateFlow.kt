package io.qent.sona

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.http.client.jdk.JdkHttpClient
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel
import dev.langchain4j.model.openai.OpenAiStreamingChatModel
import io.qent.sona.core.model.TokenUsageInfo
import io.qent.sona.core.presets.Presets
import io.qent.sona.core.state.State
import io.qent.sona.core.state.StateProvider
import io.qent.sona.core.state.UiMessage
import io.qent.sona.repositories.*
import io.qent.sona.tools.PluginExternalTools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import java.io.File
import java.net.JarURLConnection
import java.net.URI
import java.net.http.HttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.time.Duration.ofSeconds
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


@Service(Service.Level.PROJECT)
class PluginStateFlow(private val project: Project) : Flow<State>, Disposable {

    private val settingsRepository = service<PluginSettingsRepository>()
    private val presetsRepository = service<PluginPresetsRepository>()
    private val chatRepository = service<PluginChatRepository>()
    private val rolesRepository = service<PluginRolesRepository>()
    private val scope = CoroutineScope(Dispatchers.Default)
    private var stateProvider: StateProvider

    private val externalTools = PluginExternalTools(project)

    var lastState: State = State.ChatState(
        messages = emptyList<UiMessage>(),
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
        onOpenServers = {},
    )

    init {
        stateProvider = StateProvider(
            presetsRepository,
            chatRepository,
            rolesRepository,
            modelFactory = { preset ->
                when (preset.provider.name) {
                    "Anthropic" -> {
                        val builder = AnthropicStreamingChatModel.builder()
                            .apiKey(preset.apiKey)
                            .baseUrl(preset.apiEndpoint)
                            .modelName(preset.model)
                            .maxTokens(8000)
                        if (settingsRepository.state.ignoreHttpsErrors) {
                            builder.httpClientBuilder(
                                JdkHttpClient.builder().httpClientBuilder(ignoreHttpsClientBuilder())
                            )
                        }
                        if (settingsRepository.state.cacheSystemPrompts) {
                            builder.cacheSystemMessages(true)
                        }
                        if (settingsRepository.state.cacheToolDescriptions) {
                            builder.cacheTools(true)
                        }
                        if (settingsRepository.state.cacheSystemPrompts || settingsRepository.state.cacheToolDescriptions) {
                            builder.beta("prompt-caching-2024-07-31")
                        }
                        builder.build()
                    }

                    "OpenAI" -> {
                        val builder = OpenAiStreamingChatModel.builder()
                            .apiKey(preset.apiKey)
                            .baseUrl(preset.apiEndpoint)
                            .modelName(preset.model)
                        if (settingsRepository.state.ignoreHttpsErrors) {
                            builder.httpClientBuilder(
                                JdkHttpClient.builder().httpClientBuilder(ignoreHttpsClientBuilder())
                            )
                        }
                        builder.build()
                    }

                    "OpenAI-like API" -> {
                        val httpClientBuilder = if (settingsRepository.state.ignoreHttpsErrors) {
                            ignoreHttpsClientBuilder()
                        } else {
                            HttpClient.newBuilder()
                        }.version(HttpClient.Version.HTTP_1_1)

                        val builder = OpenAiStreamingChatModel.builder()
                            .apiKey("none")
                            .baseUrl(preset.apiEndpoint)
                            .modelName(preset.model)
                            .timeout(ofSeconds(60))
                            .httpClientBuilder(JdkHttpClient.builder().httpClientBuilder(httpClientBuilder))
                        builder.build()
                    }

                    "Deepseek" -> {
                        val builder = OpenAiStreamingChatModel.builder()
                            .apiKey(preset.apiKey)
                            .baseUrl(preset.apiEndpoint)
                            .modelName(preset.model)
                        if (settingsRepository.state.ignoreHttpsErrors) {
                            builder.httpClientBuilder(
                                JdkHttpClient.builder().httpClientBuilder(ignoreHttpsClientBuilder())
                            )
                        }
                        builder.build()
                    }

                    "Gemini" -> {
                        val builder = GoogleAiGeminiStreamingChatModel.builder()
                            .apiKey(preset.apiKey)
                            .baseUrl(preset.apiEndpoint)
                            .modelName(preset.model)
                        if (settingsRepository.state.ignoreHttpsErrors) {
                            builder.httpClientBuilder(
                                JdkHttpClient.builder().httpClientBuilder(ignoreHttpsClientBuilder())
                            )
                        }
                        builder.build()
                    }
                    else -> throw IllegalArgumentException("Unknown provider ${preset.provider.name}")
                }
            },
            externalTools = externalTools,
            filePermissionRepository = PluginFilePermissionsRepository(project),
            mcpServersRepository = project.service<PluginMcpServersRepository>(),
            settingsRepository = settingsRepository,
            editConfig = { project.service<PluginMcpServersRepository>().openConfig() },
            scope = scope,
            systemMessages = createSystemMessages(),
            logger = IdeaLogger,
        )

        stateProvider.state.onEach {
            lastState = it
        }.launchIn(scope)
    }

    private fun ignoreHttpsClientBuilder(): HttpClient.Builder {
        val trustAll: Array<TrustManager> = arrayOf(object : X509TrustManager {
            override fun checkClientTrusted(certs: Array<X509Certificate?>?, authType: String?) = Unit
            override fun checkServerTrusted(certs: Array<X509Certificate?>?, authType: String?) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate?>? = arrayOfNulls(0)
        })
        val sslContext = SSLContext.getInstance("SSL").apply {
            init(null, trustAll, SecureRandom())
        }
        return HttpClient.newBuilder().sslContext(sslContext)
    }

    private fun createSystemMessages(): List<SystemMessage> {
        val env = SystemMessage.from(environmentInfo())
        return listOf(env) + loadPromptMessages()
    }

    private fun loadPromptMessages(): List<SystemMessage> {
        val messages = mutableListOf<SystemMessage>()
        val dirUrl = this::class.java.classLoader.getResource("prompts/system")
        if (dirUrl != null) {
            messages += when (dirUrl.protocol) {
                "jar" -> {
                    val connection = dirUrl.openConnection() as? JarURLConnection ?: return emptyList()
                    val jarUri = connection.jarFileURL.toURI()
                    val fileSystemUri = if (jarUri.scheme == "file") {
                        URI.create("jar:${jarUri}")
                    } else jarUri
                    FileSystems.newFileSystem(fileSystemUri, emptyMap<String, Any>()).use { fs ->
                        val path = fs.getPath("prompts", "system")
                        loadMessagesFromPath(path)
                    }
                }
                else -> {
                    val path = Paths.get(dirUrl.toURI())
                    loadMessagesFromPath(path)
                }
            }
        }
        if (isMemoryServerEnabled()) {
            this::class.java.classLoader.getResourceAsStream("prompts/memory_instructions.md")?.use {
                messages += SystemMessage.from(it.reader().readText())
            }
        }
        return messages
    }

    private fun isMemoryServerEnabled(): Boolean {
        val repo = project.service<PluginMcpServersRepository>()
        return runBlocking { repo.loadEnabled().contains("memory") }
    }

    private fun loadMessagesFromPath(path: Path): List<SystemMessage> {
        val messages = mutableListOf<SystemMessage>()
        Files.list(path).use { stream ->
            stream.filter { Files.isRegularFile(it) && it.toString().endsWith(".md") }
                .forEach { p -> messages += SystemMessage.from(Files.readString(p)) }
        }
        return messages
    }

    private fun environmentInfo(): String {
        val os = "${System.getProperty("os.name")} ${System.getProperty("os.version")}".trim()
        val ide = ApplicationInfo.getInstance().fullApplicationName
        val java = System.getProperty("java.version")
        val python = runCommand("python", "--version")
        val node = runCommand("node", "--version")
        val base = project.basePath?.let { File(it) }
        val builds = detectBuildSystems(base)
        return listOf(
            "OS: $os",
            "IDE: $ide",
            "Java: $java",
            "Python: $python",
            "Node: $node",
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

    override suspend fun collect(collector: FlowCollector<State>) {
        stateProvider.state.collect(collector)
    }

    override fun dispose() {
        stateProvider.dispose()
        scope.cancel()
    }
}