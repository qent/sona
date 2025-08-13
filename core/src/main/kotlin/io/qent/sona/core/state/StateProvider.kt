package io.qent.sona.core.state

import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.model.chat.StreamingChatModel
import io.qent.sona.core.chat.Chat
import io.qent.sona.core.chat.ChatAgentFactory
import io.qent.sona.core.chat.ChatController
import io.qent.sona.core.chat.ChatRepository
import io.qent.sona.core.chat.ChatStateFlow
import io.qent.sona.core.chat.PermissionedToolExecutor
import io.qent.sona.core.chat.ToolsMapFactory
import io.qent.sona.core.mcp.McpConnectionManager
import io.qent.sona.core.mcp.McpServersRepository
import io.qent.sona.core.permissions.FilePermissionManager
import io.qent.sona.core.permissions.FilePermissionsRepository
import io.qent.sona.core.presets.Preset
import io.qent.sona.core.presets.Presets
import io.qent.sona.core.presets.PresetsRepository
import io.qent.sona.core.presets.PresetsStateFlow
import io.qent.sona.core.roles.Roles
import io.qent.sona.core.roles.RolesRepository
import io.qent.sona.core.roles.RolesStateFlow
import io.qent.sona.core.settings.SettingsRepository
import io.qent.sona.core.prompts.UserPromptRepository
import io.qent.sona.core.state.interactors.ChatStateInteractor
import io.qent.sona.core.state.interactors.EditPresetStateInteractor
import io.qent.sona.core.state.interactors.EditRoleStateInteractor
import io.qent.sona.core.state.interactors.PresetsListStateInteractor
import io.qent.sona.core.state.interactors.RolesListStateInteractor
import io.qent.sona.core.state.interactors.ServersController
import io.qent.sona.core.state.interactors.ServersStateInteractor
import io.qent.sona.core.tools.ExternalTools
import io.qent.sona.core.tools.InternalTools
import io.qent.sona.core.tools.Tools
import io.qent.sona.core.tools.ToolsInfoDecorator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

import io.qent.sona.core.Logger

class StateProvider(
    presetsRepository: PresetsRepository,
    chatRepository: ChatRepository,
    rolesRepository: RolesRepository,
    private val userPromptRepository: UserPromptRepository,
    modelFactory: (Preset) -> StreamingChatModel,
    externalTools: ExternalTools,
    filePermissionRepository: FilePermissionsRepository,
    mcpServersRepository: McpServersRepository,
    settingsRepository: SettingsRepository,
    private val editConfig: () -> Unit,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    systemMessages: () -> List<SystemMessage> = { emptyList() },
    logger: Logger = Logger.NoOp,
) {
    private val filePermissionManager = FilePermissionManager(filePermissionRepository)
    private val internalTools = object : InternalTools {
        override fun switchRole(name: String): String {
            scope.launch {
                rolesListInteractor.selectRole(name)
            }
            return "$name role active"
        }
    }
    val chatStateFlow = ChatStateFlow(chatRepository, scope)
    private val tools: Tools = ToolsInfoDecorator(chatStateFlow, internalTools, externalTools, filePermissionManager)
    private val log = object : Logger {
        override fun log(message: String) {
            runBlocking {
                if (settingsRepository.load().enablePluginLogging) logger.log("[Sona] $message")
            }
        }
    }
    private val mcpManager = McpConnectionManager(mcpServersRepository, scope, log)
    private val permissionedToolExecutor = PermissionedToolExecutor(chatStateFlow, chatRepository, log)
    private val toolsMapFactory = ToolsMapFactory(
        chatStateFlow,
        tools,
        mcpManager,
        permissionedToolExecutor,
        rolesRepository,
        presetsRepository
    )
    private val chatAgentFactory = ChatAgentFactory(
        modelFactory,
        systemMessages,
        toolsMapFactory,
        presetsRepository,
        rolesRepository,
        chatRepository
    )
    private val chatController = ChatController(
        presetsRepository,
        chatRepository,
        settingsRepository,
        chatStateFlow,
        chatAgentFactory,
        scope,
        log
    )

    private val chatInteractor = ChatStateInteractor(chatController, chatRepository, chatStateFlow)
    private val rolesFlow = RolesStateFlow(rolesRepository)
    private val rolesListInteractor = RolesListStateInteractor(rolesFlow)
    private val editRoleInteractor = EditRoleStateInteractor(rolesFlow)
    private val presetsFlow = PresetsStateFlow(presetsRepository)
    private val presetsListInteractor = PresetsListStateInteractor(presetsFlow)
    private val editPresetInteractor = EditPresetStateInteractor(presetsFlow)
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

    init {
        startChatStateEmitting()

        scope.launch {
            rolesListInteractor.load()
            presetsListInteractor.load()
            val lastChatId = chatInteractor.listChats().firstOrNull()?.id
            when {
                presetsFlow.value.presets.isEmpty() -> {
                    editPresetInteractor.startCreate()
                    emitEditPresetState()
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
        chatScreenJob?.cancel()
        chatScreenJob = combine(chatStateFlow, rolesFlow, presetsFlow) { chat, roles, presets ->
            emitChatState(chat, roles, presets)
        }.launchIn(scope)
    }

    private suspend fun emitChatState(chat: Chat, roles: Roles, presets: Presets) {
        val state = factory.createChatState(
            chat = chat,
            roles = roles,
            presets = presets,
            onSelectRole = { idx -> scope.launch { rolesListInteractor.selectRole(idx) } },
            onSelectPreset = { idx ->
                scope.launch {
                    presetsListInteractor.selectPreset(idx)
                }
            },
            onSendMessage = { text ->
                scope.launch {
                    if (presets.presets.isEmpty()) {
                        editPresetInteractor.startCreate(); emitEditPresetState()
                    } else {
                        chatInteractor.send(text)
                    }
                }
            },
            onStop = { chatInteractor.stop() },
            onDeleteFrom = { idx -> scope.launch { chatInteractor.deleteFrom(idx) } },
            onToggleAutoApprove = { scope.launch { chatInteractor.toggleAutoApproveTools() } },
            onAllowTool = { scope.launch { permissionedToolExecutor.resolveToolPermission(true, false) } },
            onAlwaysAllowTool = { scope.launch { permissionedToolExecutor.resolveToolPermission(true, true) } },
            onDenyTool = { scope.launch { permissionedToolExecutor.resolveToolPermission(false, false) } },
            onNewChat = { scope.launch { chatInteractor.newChat() } },
            onOpenHistory = { scope.launch { showHistory() } },
            onOpenRoles = { scope.launch { showRoles() } },
            onOpenPresets = { scope.launch { showPresets() } },
            onOpenServers = { scope.launch { showServers() } },
            onOpenUserPrompt = { scope.launch { showUserPrompt() } },
        )
        _state.emit(state)
    }

    private suspend fun showHistory() {
        chatScreenJob?.cancel()

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
            onOpenUserPrompt = { scope.launch { showUserPrompt() } },
        )
        _state.emit(state)
    }

    private suspend fun showRoles() {
        chatScreenJob?.cancel()
        rolesListInteractor.load()
        emitRolesListState()
    }

    private suspend fun emitRolesListState() {
        val roles = rolesListInteractor.roles
        val state = factory.createRolesListState(
            roles = roles,
            onSelectRole = { idx -> scope.launch { rolesListInteractor.selectRole(idx); emitRolesListState() } },
            onAddRole = { editRoleInteractor.startCreate(); scope.launch { emitEditRoleState() } },
            onEditRole = { idx -> editRoleInteractor.startEdit(idx); scope.launch { emitEditRoleState() } },
            onDeleteRole = { idx -> scope.launch { rolesListInteractor.deleteRole(idx); emitRolesListState() } },
            onNewChat = {
                startChatStateEmitting()
                scope.launch { chatInteractor.newChat() }
            },
            onOpenHistory = { scope.launch { showHistory() } },
            onOpenPresets = { scope.launch { showPresets() } },
            onOpenServers = { scope.launch { showServers() } },
            onOpenUserPrompt = { scope.launch { showUserPrompt() } },
        )
        _state.emit(state)
    }

    private suspend fun emitEditRoleState() {
        val state = factory.createEditRoleState(
            role = editRoleInteractor.role,
            onSave = { r ->
                scope.launch {
                    editRoleInteractor.save(r)
                    emitRolesListState()
                }
            },
            onCancel = { scope.launch { emitRolesListState() } },
            onNewChat = {
                startChatStateEmitting()
                scope.launch { chatInteractor.newChat() }
            },
            onOpenHistory = { scope.launch { showHistory() } },
            onOpenPresets = { scope.launch { showPresets() } },
            onOpenServers = { scope.launch { showServers() } },
            onOpenUserPrompt = { scope.launch { showUserPrompt() } },
        )
        _state.emit(state)
    }

    private suspend fun showPresets() {
        chatScreenJob?.cancel()
        presetsListInteractor.load()
        chatScreenJob = presetsFlow.onEach { presets ->
            if (presets.presets.isEmpty()) {
                editPresetInteractor.startCreate()
                emitEditPresetState()
            } else {
                emitPresetsListState(presets)
            }
        }.launchIn(scope)
    }

    private suspend fun emitPresetsListState(presets: Presets) {
        val state = factory.createPresetsListState(
            presets = presets,
            onSelectPreset = { idx -> scope.launch { presetsListInteractor.selectPreset(idx) } },
            onAddPreset = { editPresetInteractor.startCreate(); scope.launch { emitEditPresetState() } },
            onEditPreset = { idx -> editPresetInteractor.startEdit(idx); scope.launch { emitEditPresetState() } },
            onDeletePreset = { idx ->
                scope.launch {
                    presetsListInteractor.deletePreset(idx)
                }
            },
            onNewChat = {
                startChatStateEmitting()
                scope.launch { chatInteractor.newChat() }
            },
            onOpenHistory = { scope.launch { showHistory() } },
            onOpenRoles = { scope.launch { showRoles() } },
            onOpenServers = { scope.launch { showServers() } },
            onOpenUserPrompt = { scope.launch { showUserPrompt() } },
        )
        _state.emit(state)
    }

    private suspend fun emitEditPresetState() {
        chatScreenJob?.cancel()
        val state = factory.createEditPresetState(
            preset = editPresetInteractor.preset,
            onSave = { p ->
                scope.launch {
                    val isNew = editPresetInteractor.isNew
                    editPresetInteractor.save(p)
                    if (isNew) {
                        startChatStateEmitting()
                        chatInteractor.newChat()
                    } else {
                        showPresets()
                    }
                }
            },
            onCancel = { scope.launch { showPresets() } },
            onNewChat = {
                startChatStateEmitting()
                scope.launch { chatInteractor.newChat() }
            },
            onOpenHistory = { scope.launch { showHistory() } },
            onOpenRoles = { scope.launch { showRoles() } },
            onOpenServers = { scope.launch { showServers() } },
            onOpenUserPrompt = { scope.launch { showUserPrompt() } },
        )
        _state.emit(state)
    }

    private suspend fun showUserPrompt() {
        chatScreenJob?.cancel()
        emitUserPromptState()
    }

    private suspend fun emitUserPromptState() {
        val text = userPromptRepository.load()
        val state = factory.createUserPromptState(
            prompt = text,
            onSave = { p ->
                scope.launch {
                    if (p.isNotBlank()) {
                        userPromptRepository.save(p)
                    } else {
                        userPromptRepository.save("")
                    }
                    startChatStateEmitting()
                }
            },
            onNewChat = {
                startChatStateEmitting()
                scope.launch { chatInteractor.newChat() }
            },
            onOpenHistory = { scope.launch { showHistory() } },
            onOpenRoles = { scope.launch { showRoles() } },
            onOpenPresets = { scope.launch { showPresets() } },
            onOpenServers = { scope.launch { showServers() } },
        )
        _state.emit(state)
    }

    private suspend fun showServers() {
        chatScreenJob?.cancel()
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
            onOpenUserPrompt = { scope.launch { showUserPrompt() } },
        )
        _state.emit(state)
    }
}

