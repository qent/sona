package io.qent.sona.core

import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.data.message.ToolExecutionResultMessage
import dev.langchain4j.model.chat.StreamingChatModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


class StateProvider(
    private val presetsRepository: PresetsRepository,
    private val chatRepository: ChatRepository,
    private val rolesRepository: RolesRepository,
    modelFactory: (Preset) -> StreamingChatModel,
    externalTools: ExternalTools,
    filePermissionRepository: FilePermissionsRepository,
    mcpServersRepository: McpServersRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val systemMessages: List<SystemMessage> = emptyList(),
) {

    private val filePermissionManager = FilePermissionManager(filePermissionRepository)
    private val internalTools = DefaultInternalTools(scope, ::selectRole)
    private val tools: Tools = ToolsInfoDecorator(internalTools, externalTools, filePermissionManager)
    private val mcpManager = McpConnectionManager(mcpServersRepository, scope)
    private val chatFlow = ChatFlow(presetsRepository, rolesRepository, chatRepository, modelFactory, tools, scope, systemMessages, mcpManager)

    private val _state = MutableSharedFlow<State>(replay = 1)

    val state: Flow<State> = _state

    private var roles: Roles = Roles(0, emptyList())
    private var creatingRole = false
    private var presets: Presets = Presets(0, emptyList())
    private var creatingPreset = false
    private var currentChat: Chat = Chat("", TokenUsageInfo())

    init {
        chatFlow.onEach { chat ->
            currentChat = chat
            _state.emit(createChatState(chat))
        }.launchIn(scope)

        scope.launch {
            roles = rolesRepository.load()
            presets = presetsRepository.load()
            val lastChatId = chatRepository.listChats().firstOrNull()?.id
            if (presets.presets.isEmpty()) {
                creatingPreset = true
                _state.emit(createPresetsState())
            } else if (lastChatId == null) {
                newChat()
            } else {
                openChat(lastChatId)
            }
        }
    }

    private fun createChatState(chat: Chat): State.ChatState {
        val lastAi = chat.messages.lastOrNull { it.message is AiMessage }
        val lastUsage = lastAi?.tokenUsage ?: TokenUsageInfo()
        return State.ChatState(
            messages = chat.messages.mapNotNull { it.toUiMessage() },
            totalTokenUsage = chat.tokenUsage,
            lastTokenUsage = lastUsage,
            isSending = chat.requestInProgress,
            roles = roles.roles.map { it.name },
            activeRole = roles.active,
            onSelectRole = { idx -> scope.launch {
                selectRole(idx)
                _state.emit(createChatState(chat))
            } },
            presets = presets,
            onSelectPreset = { idx -> scope.launch { selectChatPreset(idx) } },
            onSendMessage = { text -> scope.launch { send(text) } },
            onStop = { scope.launch { stop() } },
            onDeleteFrom = { idx -> scope.launch { deleteFrom(idx) } },
            toolRequest = chat.toolRequest != null,
            autoApproveTools = chat.autoApproveTools,
            onToggleAutoApprove = { scope.launch { chatFlow.toggleAutoApproveTools() } },
            onAllowTool = { scope.launch { chatFlow.resolveToolPermission(true, false) } },
            onAlwaysAllowTool = { scope.launch { chatFlow.resolveToolPermission(true, true) } },
            onDenyTool = { scope.launch { chatFlow.resolveToolPermission(false, false) } },
            onNewChat = { scope.launch { newChat() } },
            onOpenHistory = { scope.launch { showHistory() } },
            onOpenRoles = { scope.launch { showRoles() } },
            onOpenPresets = { scope.launch { showPresets() } },
            onOpenServers = { scope.launch { showServers() } },
        )
    }

    private fun createListState(chats: List<ChatSummary>) = State.ChatListState(
        chats = chats.filter { it.messages != 0 }.map { it.toUi() },
        onOpenChat = { id -> scope.launch { openChat(id) } },
        onDeleteChat = { id -> scope.launch { deleteChat(id) } },
        onNewChat = { scope.launch { newChat() } },
        onOpenRoles = { scope.launch { showRoles() } },
        onOpenPresets = { scope.launch { showPresets() } },
        onOpenServers = { scope.launch { showServers() } },
    )

    private fun createRolesState(): State.RolesState {
        val text = if (creatingRole) "" else roles.roles[roles.active].text
        return State.RolesState(
            roles = roles.roles.map { it.name },
            currentIndex = roles.active,
            creating = creatingRole,
            text = text,
            onSelectRole = { idx -> scope.launch {
                selectRole(idx)
                _state.emit(createRolesState())
            } },
            onStartCreateRole = { scope.launch { startCreateRole() } },
            onAddRole = { name, t -> scope.launch { addRole(name, t) } },
            onDeleteRole = { scope.launch { deleteRole() } },
            onSave = { t -> scope.launch { saveRole(t) } },
            onNewChat = { scope.launch { newChat() } },
            onOpenHistory = { scope.launch { showHistory() } },
            onOpenRoles = { },
            onOpenPresets = { scope.launch { showPresets() } },
            onOpenServers = { scope.launch { showServers() } },
        )
    }

    private suspend fun showHistory() {
        val chats = chatRepository.listChats()
        _state.emit(createListState(chats))
    }

    private suspend fun showRoles() {
        roles = rolesRepository.load()
        creatingRole = false
        _state.emit(createRolesState())
    }

    private suspend fun showPresets() {
        presets = presetsRepository.load()
        if (presets.presets.isEmpty()) creatingPreset = true
        _state.emit(createPresetsState())
    }

    private suspend fun showServers() {
        _state.emit(createServersState())
    }

    private suspend fun newChat() {
        val lastChat = chatRepository.listChats().firstOrNull()?.id ?: run {
            chatFlow.loadChat(chatRepository.createChat())
            return
        }

        if (chatRepository.loadMessages(lastChat).isEmpty()) {
            openChat(lastChat)
        } else {
            chatFlow.loadChat(chatRepository.createChat())
        }
    }

    private suspend fun openChat(id: String) = chatFlow.loadChat(id)

    private suspend fun saveRole(text: String) {
        val list = roles.roles.toMutableList()
        list[roles.active] = list[roles.active].copy(text = text)
        roles = roles.copy(roles = list)
        rolesRepository.save(roles)
        _state.emit(createRolesState())
    }

    private suspend fun selectRole(idx: Int) {
        roles = roles.copy(active = idx)
        rolesRepository.save(roles)
    }

    suspend fun selectRole(role: DefaultRoles) {
        val idx = roles.roles.indexOfFirst { it.name == role.displayName }
        if (idx >= 0) {
            selectRole(idx)
            _state.emit(createChatState(currentChat))
        }
    }

    private suspend fun startCreateRole() {
        creatingRole = true
        _state.emit(createRolesState())
    }

    private suspend fun addRole(name: String, text: String) {
        roles = Roles(
            active = roles.roles.size,
            roles = roles.roles + Role(name, text)
        )
        creatingRole = false
        rolesRepository.save(roles)
        _state.emit(createRolesState())
    }

    private suspend fun deleteRole() {
        val currentName = roles.roles[roles.active].name
        if (currentName in DefaultRoles.NAMES) return
        val list = roles.roles.toMutableList()
        list.removeAt(roles.active)
        val newActive = roles.active.coerceAtMost(list.lastIndex)
        roles = Roles(active = newActive, roles = list)
        rolesRepository.save(roles)
        _state.emit(createRolesState())
    }

    private suspend fun deleteChat(id: String) {
        chatRepository.deleteChat(id)
        if (chatRepository.listChats().isEmpty()) {
            newChat()
        } else {
            showHistory()
        }
    }

    private suspend fun send(text: String) {
        if (presets.presets.isEmpty()) {
            creatingPreset = true
            _state.emit(createPresetsState())
        } else {
            chatFlow.send(text)
        }
    }
    private fun stop() {
        chatFlow.stop()
        mcpManager.stop()
    }
    private suspend fun deleteFrom(idx: Int) = chatFlow.deleteFrom(idx)

    private fun createPresetsState(): State.PresetsState {
        val emptyPresets = presets.presets.isEmpty()
        val preset = if (creatingPreset || emptyPresets) {
            Preset(
                name = "Sonnet 4.0",
                provider = LlmProvider.Anthropic,
                apiEndpoint = LlmProvider.Anthropic.defaultEndpoint,
                model = LlmProvider.Anthropic.models.first().name,
                apiKey = "",
            )
        } else {
            presets.presets[presets.active]
        }
        return State.PresetsState(
            presets = presets.presets.map { it.name },
            currentIndex = presets.active,
            creating = creatingPreset || emptyPresets,
            preset = preset,
            onSelectPreset = { idx -> scope.launch { selectPreset(idx) } },
            onStartCreatePreset = { scope.launch { startCreatePreset() } },
            onAddPreset = { p -> scope.launch { addPreset(p) } },
            onDeletePreset = { scope.launch { deletePreset() } },
            onSave = { p -> scope.launch { savePreset(p) } },
            onNewChat = { scope.launch { newChat() } },
            onOpenHistory = { scope.launch { showHistory() } },
            onOpenRoles = { scope.launch { showRoles() } },
            onOpenServers = { scope.launch { showServers() } },
        )
    }

    private fun createServersState(): State.ServersState = State.ServersState(
        servers = mcpManager.servers,
        onToggleServer = { name -> mcpManager.toggle(name) },
        onReload = { scope.launch { mcpManager.reload() } },
        onNewChat = { scope.launch { newChat() } },
        onOpenHistory = { scope.launch { showHistory() } },
        onOpenRoles = { scope.launch { showRoles() } },
        onOpenPresets = { scope.launch { showPresets() } },
    )

    private suspend fun selectChatPreset(idx: Int) {
        presets = presets.copy(active = idx)
        presetsRepository.save(presets)
        _state.emit(createChatState(currentChat))
    }

    private suspend fun selectPreset(idx: Int) {
        presets = presets.copy(active = idx)
        presetsRepository.save(presets)
        _state.emit(createPresetsState())
    }

    private suspend fun startCreatePreset() {
        creatingPreset = true
        _state.emit(createPresetsState())
    }

    private suspend fun addPreset(preset: Preset) {
        presets = Presets(
            active = presets.presets.size,
            presets = presets.presets + preset,
        )
        creatingPreset = false
        presetsRepository.save(presets)

        newChat()
    }

    private suspend fun deletePreset() {
        if (presets.presets.isEmpty()) return
        val list = presets.presets.toMutableList()
        list.removeAt(presets.active)
        val newActive = presets.active.coerceAtMost(list.lastIndex).coerceAtLeast(0)
        presets = Presets(newActive, list)
        presetsRepository.save(presets)
        _state.emit(createPresetsState())
    }

    private suspend fun savePreset(preset: Preset) {
        val list = presets.presets.toMutableList()
        if (list.isNotEmpty()) {
            list[presets.active] = preset
            presets = presets.copy(presets = list)
            presetsRepository.save(presets)
        }
        _state.emit(createPresetsState())
    }
}

private fun ChatRepositoryMessage.toUiMessage(): UiMessage? = when (val m = message) {
    is AiMessage -> UiMessage.Ai(m.text().orEmpty(), m.toolExecutionRequests())
    is UserMessage -> UiMessage.User(m.singleText().trim())
    is ToolExecutionResultMessage -> UiMessage.Tool(m.text())
    else -> null
}

private fun ChatSummary.toUi(): UiChatSummary =
    UiChatSummary(id, firstMessage, messages, createdAt)
