package io.qent.sona.repositories

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import io.qent.sona.core.prompts.UserPromptRepository

@Service
@State(name = "PluginUserPrompt", storages = [Storage("user_prompt.xml")])
class PluginUserPromptRepository : UserPromptRepository,
    PersistentStateComponent<PluginUserPromptRepository.UserPromptState> {

    data class UserPromptState(var prompt: String = "")

    private var state = UserPromptState()

    override fun getState() = state

    override fun loadState(state: UserPromptState) {
        this.state = state
    }

    override suspend fun load(): String = state.prompt

    override suspend fun save(prompt: String) {
        state.prompt = prompt
    }
}
