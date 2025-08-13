package io.qent.sona.repositories

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service
@State(name = "PatchStorage", storages = [Storage("patch_storage.xml")])
class PatchStorage : PersistentStateComponent<PatchStorage.State> {

    data class ChatPatches(
        var chatId: String = "",
        var nextId: Int = 1,
        var patches: MutableMap<Int, String> = mutableMapOf(),
    )

    data class State(
        var chats: MutableList<ChatPatches> = mutableListOf(),
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    private fun findChat(chatId: String): ChatPatches {
        return state.chats.firstOrNull { it.chatId == chatId }
            ?: ChatPatches(chatId).also { state.chats.add(it) }
    }

    fun createPatch(chatId: String, patch: String): Int {
        val chat = findChat(chatId)
        val id = chat.nextId++
        chat.patches[id] = patch
        return id
    }

    fun getPatch(chatId: String, id: Int): String? {
        return state.chats.firstOrNull { it.chatId == chatId }?.patches?.get(id)
    }

    fun deletePatches(chatId: String) {
        state.chats.removeIf { it.chatId == chatId }
    }
}
