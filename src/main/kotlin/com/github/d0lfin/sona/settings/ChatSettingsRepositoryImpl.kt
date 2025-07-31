package com.github.d0lfin.sona.settings

import com.github.d0lfin.sona.logic.ChatSettings
import com.github.d0lfin.sona.logic.ChatSettingsRepository
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.PersistentStateComponent

@Service
@State(name = "ChatSettings", storages = [Storage("chat_settings.xml")])
class ChatSettingsRepositoryImpl : ChatSettingsRepository, PersistentStateComponent<ChatSettingsRepositoryImpl.State> {
    data class State(var apiKey: String = "", var baseUrl: String = "", var model: String = "")

    var state: State = State()

    override fun getState(): State = state
    override fun loadState(state: State) { this.state = state }

    override suspend fun load(): ChatSettings = ChatSettings(state.apiKey, state.baseUrl, state.model)

    override suspend fun save(settings: ChatSettings) {
        state = State(settings.apiKey, settings.baseUrl, settings.model)
    }
}
