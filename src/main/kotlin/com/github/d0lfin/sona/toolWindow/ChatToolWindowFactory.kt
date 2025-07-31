package com.github.d0lfin.sona.toolWindow

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.d0lfin.sona.logic.ChatLogic
import com.github.d0lfin.sona.logic.ChatSettings
import com.github.d0lfin.sona.logic.PluginState
import com.github.d0lfin.sona.settings.ChatSettingsRepositoryImpl
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import dev.langchain4j.model.anthropic.AnthropicChatModel
import org.jetbrains.jewel.bridge.JewelComposePanel
import org.jetbrains.jewel.ui.component.Button
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope

class ChatToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val repository = service<ChatSettingsRepositoryImpl>()
        val scope = CoroutineScope(Dispatchers.Default)
        val logic = ChatLogic(repository, modelFactory = { settings ->
            AnthropicChatModel.builder()
                .apiKey(settings.apiKey)
                .baseUrl(settings.baseUrl)
                .modelName(settings.model)
                .build()
        }, scope = scope)
        val content = ContentFactory.getInstance().createContent(ChatPanel(logic).component(), null, false)
        toolWindow.contentManager.addContent(content)
    }
}

private class ChatPanel(private val logic: ChatLogic) {
    fun component() = JewelComposePanel { Ui() }

    @Composable
    private fun Ui() {
        val state by logic.state.collectAsState()
        Column(Modifier.fillMaxSize()) {
            Header(state)
            Messages(state)
            Input(state)
        }
    }

    @Composable
    private fun Header(state: PluginState) {
        Row(Modifier.fillMaxWidth().padding(8.dp)) {
            Text("Out: ${'$'}{state.outgoingTokens}  In: ${'$'}{state.incomingTokens}")
        }
    }

    @Composable
    private fun Messages(state: PluginState) {
        val scroll = rememberScrollState()
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(8.dp)) {
            state.messages.forEach { msg ->
                Text(msg.toString())
                Spacer(Modifier.height(4.dp))
            }
        }
    }

    @Composable
    private fun Input(state: PluginState) {
        var text by remember { mutableStateOf("") }
        Row(Modifier.fillMaxWidth().padding(8.dp)) {
            TextField(text, onValueChange = { text = it }, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                state.onSendMessage(text)
                text = ""
            }, enabled = !state.isSending) { Text("Send") }
        }
    }
}
