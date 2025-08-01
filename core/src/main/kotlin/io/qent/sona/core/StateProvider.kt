package io.qent.sona.core

import dev.langchain4j.data.message.ChatMessageType
import dev.langchain4j.model.chat.StreamingChatModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import io.qent.sona.core.Role
import io.qent.sona.core.Roles


class StateProvider(
    settingsRepository: SettingsRepository,
    private val chatRepository: ChatRepository,
    private val rolesRepository: RolesRepository,
    modelFactory: suspend (Settings) -> StreamingChatModel,
    tools: Tools,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {

    private val chatFlow = ChatFlow(settingsRepository, rolesRepository, chatRepository, modelFactory, tools, scope)

    private val _state = MutableSharedFlow<State>()
    val state: Flow<State> = _state

    private var roles: Roles = Roles()
    private var creatingRole = false

    init {
        scope.launch {
            val lastChatId = chatRepository.listChats().firstOrNull()?.id
            if (lastChatId == null) {
                newChat()
            } else {
                openChat(lastChatId)
            }
        }

        chatFlow.onEach { (chatId, tokenUsage, messages, requestInProgress) ->
            _state.emit(State.ChatState(
                messages.filter { it.message.type() != ChatMessageType.SYSTEM }.map { it.message },
                tokenUsage.outputTokenCount(),
                tokenUsage.inputTokenCount(),
                isSending = requestInProgress,
                onSendMessage = { text -> scope.launch { send(text) } },
                onNewChat = { scope.launch { newChat() } },
                onOpenHistory = { scope.launch { showHistory() } },
                onOpenRoles = { scope.launch { showRoles() } },
            ))
        }.launchIn(scope)

        chatFlow.buffer(10).filter { !it.requestInProgress }
            .map { it.messages.lastOrNull() }
            .filterNotNull()
            .distinctUntilChanged()
            .onEach { message ->
                chatRepository.addMessage(
                    message.chatId,
                    message.message,
                    message.model,
                    message.inputTokens,
                    message.outputTokens
                )
            }
            .launchIn(scope)
    }

    private fun createListState(chats: List<ChatSummary>) = State.ChatListState(
        chats = chats,
        onOpenChat = { id -> scope.launch { openChat(id) } },
        onDeleteChat = { id -> scope.launch { deleteChat(id) } },
        onNewChat = { scope.launch { newChat() } },
        onOpenRoles = { scope.launch { showRoles() } },
    )

    private fun createRolesState(): State.RolesState {
        val text = if (creatingRole) "" else roles.roles[roles.active].text
        return State.RolesState(
            roles = roles.roles.map { it.name },
            currentIndex = roles.active,
            creating = creatingRole,
            text = text,
            onSelectRole = { idx -> scope.launch { selectRole(idx) } },
            onStartCreateRole = { scope.launch { startCreateRole() } },
            onAddRole = { name, t -> scope.launch { addRole(name, t) } },
            onDeleteRole = { scope.launch { deleteRole() } },
            onSave = { t -> scope.launch { saveRole(t) } },
            onNewChat = { scope.launch { newChat() } },
            onOpenHistory = { scope.launch { showHistory() } }
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

    private suspend fun newChat() = chatFlow.loadChat(chatRepository.createChat())

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
        _state.emit(createRolesState())
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
        if (roles.roles.size <= 1) return
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

    private suspend fun send(text: String) = chatFlow.send(text)
}
