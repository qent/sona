package io.qent.sona.core.state

import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.model.chat.StreamingChatModel
import io.qent.sona.core.chat.Chat
import io.qent.sona.core.chat.ChatFlow
import io.qent.sona.core.chat.ChatRepository
import io.qent.sona.core.mcp.McpConnectionManager
import io.qent.sona.core.mcp.McpServersRepository
import io.qent.sona.core.permissions.FilePermissionManager
import io.qent.sona.core.permissions.FilePermissionsRepository
import io.qent.sona.core.presets.LlmProviders
import io.qent.sona.core.presets.Preset
import io.qent.sona.core.presets.Presets
import io.qent.sona.core.presets.PresetsRepository
import io.qent.sona.core.roles.Roles
import io.qent.sona.core.roles.RolesRepository
import io.qent.sona.core.settings.SettingsRepository
import io.qent.sona.core.tools.ExternalTools
import io.qent.sona.core.tools.InternalTools
import io.qent.sona.core.tools.Tools
import io.qent.sona.core.tools.ToolsInfoDecorator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StateProvider(
    presetsRepository: PresetsRepository,
    chatRepository: ChatRepository,
    rolesRepository: RolesRepository,
    modelFactory: (Preset) -> StreamingChatModel,
    externalTools: ExternalTools,
    filePermissionRepository: FilePermissionsRepository,
    mcpServersRepository: McpServersRepository,
    private val settingsRepository: SettingsRepository,
    private val editConfig: () -> Unit,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    systemMessages: List<SystemMessage> = emptyList(),
) {
    private val filePermissionManager = FilePermissionManager(filePermissionRepository)
    private val internalTools = object : InternalTools {
        override fun switchRole(name: String): String {
            scope.launch {
                rolesInteractor.selectRole(name)
            }
            return "$name role active"
        }
    }
    private val tools: Tools = ToolsInfoDecorator(internalTools, externalTools, filePermissionManager)
    private val mcpManager = McpConnectionManager(mcpServersRepository, scope)
    private val chatFlow = ChatFlow(
        presetsRepository,
        rolesRepository,
        chatRepository,
        modelFactory,
        tools,
        scope,
        systemMessages,
        mcpManager,
        settingsRepository
    )

    private val chatInteractor = ChatStateInteractor(object : ChatSession, Flow<Chat> by chatFlow {
        override suspend fun loadChat(id: String) = chatFlow.loadChat(id)
        override suspend fun send(text: String) = chatFlow.send(text)
        override fun stop() = chatFlow.stop()
        override suspend fun deleteFrom(idx: Int) = chatFlow.deleteFrom(idx)
        override fun toggleAutoApproveTools() = chatFlow.toggleAutoApproveTools()
        override suspend fun resolveToolPermission(allow: Boolean, always: Boolean) =
            chatFlow.resolveToolPermission(allow, always)
    }, chatRepository)
    private val rolesFlow = RolesStateFlow(rolesRepository)
    private val rolesInteractor = RolesStateInteractor(rolesFlow)
    private val presetsInteractor = PresetsStateInteractor(presetsRepository)
    private val serversInteractor = ServersStateInteractor(object : ServersController {
        override val servers = mcpManager.servers
        override fun toggle(name: String) = mcpManager.toggle(name)
        override fun toggleTool(server: String, tool: String) = mcpManager.toggleTool(server, tool)
        override suspend fun reload() = mcpManager.reload()
        override fun stop() = mcpManager.stop()
    })
    private val factory = StateFactory()

    private val _state = MutableSharedFlow<State>(replay = 1)
    val state: Flow<State> = _state

    private var chatScreenJob: Job? = null
    private var rolesScreenJob: Job? = null

    init {
        startChatStateEmitting()

        scope.launch {
            rolesInteractor.load()
            presetsInteractor.load()
            val lastChatId = chatInteractor.listChats().firstOrNull()?.id
            when {
                presetsInteractor.presets.presets.isEmpty() -> {
                    presetsInteractor.startCreatePreset()
                    emitPresetsState()
                }

                lastChatId == null -> chatInteractor.newChat()
                else -> chatInteractor.openChat(lastChatId)
            }
        }
    }

    fun dispose() {
        serversInteractor.stop()
    }

    private fun startChatStateEmitting() {
        rolesScreenJob?.cancel()
        rolesScreenJob = null

        chatScreenJob?.cancel()
        chatScreenJob = combine(chatFlow, rolesFlow) { chat, roles ->
            emitChatState(chat, roles)
        }.launchIn(scope)
    }

    private fun startRolesStateEmitting() {
        chatScreenJob?.cancel()
        chatScreenJob = null
        rolesScreenJob?.cancel()
        rolesScreenJob = rolesFlow.onEach { roles -> emitRolesState(roles) }.launchIn(scope)
    }

    private suspend fun emitChatState(chat: Chat, roles: Roles) {
        val presets: Presets = presetsInteractor.presets
        val state = factory.createChatState(
            chat = chat,
            roles = roles,
            presets = presets,
            onSelectRole = { idx -> scope.launch { rolesInteractor.selectRole(idx) } },
            onSelectPreset = { idx ->
                scope.launch {
                    presetsInteractor.selectPreset(idx); emitChatState(
                    chat,
                    roles
                )
                }
            },
            onSendMessage = { text ->
                scope.launch {
                    if (presetsInteractor.presets.presets.isEmpty()) {
                        presetsInteractor.startCreatePreset(); emitPresetsState()
                    } else {
                        chatInteractor.send(text)
                    }
                }
            },
            onStop = { chatInteractor.stop() },
            onDeleteFrom = { idx -> scope.launch { chatInteractor.deleteFrom(idx) } },
            onToggleAutoApprove = { scope.launch { chatInteractor.toggleAutoApproveTools() } },
            onAllowTool = { scope.launch { chatInteractor.resolveToolPermission(true, false) } },
            onAlwaysAllowTool = { scope.launch { chatInteractor.resolveToolPermission(true, true) } },
            onDenyTool = { scope.launch { chatInteractor.resolveToolPermission(false, false) } },
            onNewChat = { scope.launch { chatInteractor.newChat() } },
            onOpenHistory = { scope.launch { showHistory() } },
            onOpenRoles = { scope.launch { showRoles() } },
            onOpenPresets = { scope.launch { showPresets() } },
            onOpenServers = { scope.launch { showServers() } },
        )
        _state.emit(state)
    }

    private suspend fun showHistory() {
        chatScreenJob?.cancel()
        rolesScreenJob?.cancel()

        val chats = chatInteractor.listChats()
        val state = factory.createChatListState(
            chats = chats,
            onOpenChat = { id ->
                startChatStateEmitting()
                scope.launch {
                    chatInteractor.openChat(id)
                }
            },
            onDeleteChat = { id ->
                scope.launch {
                    chatInteractor.deleteChat(id)
                    showHistory()
                }
            },
            onNewChat = {
                startChatStateEmitting()
                scope.launch {
                    chatInteractor.newChat()
                }
            },
            onOpenRoles = { scope.launch { showRoles() } },
            onOpenPresets = { scope.launch { showPresets() } },
            onOpenServers = { scope.launch { showServers() } },
        )
        _state.emit(state)
    }

    private suspend fun showRoles() {
        chatScreenJob?.cancel()
        rolesInteractor.load()
        rolesInteractor.finishCreateRole()
        startRolesStateEmitting()
    }

    private suspend fun emitRolesState(roles: Roles) {
        val creating = rolesInteractor.creatingRole
        val short = if (creating) "" else roles.roles[roles.active].short
        val text = if (creating) "" else roles.roles[roles.active].text
        val state = factory.createRolesState(
            roles = roles,
            creatingRole = creating,
            short = short,
            text = text,
            onSelectRole = { idx -> scope.launch { rolesInteractor.selectRole(idx) } },
            onStartCreateRole = {
                rolesInteractor.startCreateRole()
                scope.launch { emitRolesState(rolesFlow.value) }
            },
            onAddRole = { name, s, t -> scope.launch { rolesInteractor.addRole(name, s, t) } },
            onDeleteRole = { scope.launch { rolesInteractor.deleteRole() } },
            onSave = { s, t -> scope.launch { rolesInteractor.saveRole(s, t) } },
            onNewChat = {
                startChatStateEmitting()
                scope.launch {
                    chatInteractor.newChat()
                }
            },
            onOpenHistory = { scope.launch { showHistory() } },
            onOpenPresets = { scope.launch { showPresets() } },
            onOpenServers = { scope.launch { showServers() } },
        )
        _state.emit(state)
    }

    private suspend fun showPresets() {
        chatScreenJob?.cancel()
        rolesScreenJob?.cancel()
        presetsInteractor.load()
        if (presetsInteractor.presets.presets.isEmpty()) presetsInteractor.startCreatePreset() else presetsInteractor.finishCreatePreset()
        emitPresetsState()
    }

    private suspend fun emitPresetsState() {
        val presets = presetsInteractor.presets
        val creating = presetsInteractor.creatingPreset
        val empty = presets.presets.isEmpty()
        val preset = if (creating || empty) {
            val defaultProvider = LlmProviders.default
            Preset(
                name = defaultProvider.models.first().name,
                provider = defaultProvider,
                apiEndpoint = defaultProvider.defaultEndpoint,
                model = defaultProvider.models.first().name,
                apiKey = "",
            )
        } else {
            presets.presets[presets.active]
        }
        val state = factory.createPresetsState(
            presets = presets,
            creatingPreset = creating || presets.presets.isEmpty(),
            preset = preset,
            onSelectPreset = { idx -> scope.launch { presetsInteractor.selectPreset(idx); emitPresetsState() } },
            onStartCreatePreset = { presetsInteractor.startCreatePreset(); scope.launch { emitPresetsState() } },
            onAddPreset = { p ->
                startChatStateEmitting()
                scope.launch {
                    presetsInteractor.addPreset(p)
                    chatInteractor.newChat()
                }
            },
            onDeletePreset = { scope.launch { presetsInteractor.deletePreset(); emitPresetsState() } },
            onSave = { p -> scope.launch { presetsInteractor.savePreset(p); emitPresetsState() } },
            onNewChat = {
                startChatStateEmitting()
                scope.launch {
                    chatInteractor.newChat()
                }
            },
            onOpenHistory = { scope.launch { showHistory() } },
            onOpenRoles = { scope.launch { showRoles() } },
            onOpenServers = { scope.launch { showServers() } },
        )
        _state.emit(state)
    }

    private suspend fun showServers() {
        chatScreenJob?.cancel()
        rolesScreenJob?.cancel()
        val state = factory.createServersState(
            servers = serversInteractor.servers,
            onToggleServer = { name -> serversInteractor.toggle(name) },
            onToggleTool = { server, tool -> serversInteractor.toggleTool(server, tool) },
            onReload = { scope.launch { serversInteractor.reload() } },
            onEditConfig = editConfig,
            onNewChat = {
                startChatStateEmitting()
                scope.launch {
                    chatInteractor.newChat()
                }
            },
            onOpenHistory = { scope.launch { showHistory() } },
            onOpenRoles = { scope.launch { showRoles() } },
            onOpenPresets = { scope.launch { showPresets() } },
        )
        _state.emit(state)
    }
}

