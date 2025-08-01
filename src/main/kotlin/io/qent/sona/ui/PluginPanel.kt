package io.qent.sona.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.UserMessage
import io.qent.sona.PluginStateFlow
import io.qent.sona.core.State.*
import org.jetbrains.jewel.ui.component.ActionButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextArea
import org.jetbrains.jewel.ui.component.TextField
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun Ui(project: Project) {
    val state = project.service<PluginStateFlow>().collectAsState()
    when (val s = state.value) {
        is ChatState -> ChatScreen(s)
        is ChatListState -> ChatListScreen(s)
        is RolesState -> RolesScreen(s)
    }
}

@Composable
private fun ChatScreen(state: ChatState) {
    Column(Modifier.fillMaxSize()) {
        Header(state)
        Messages(state, modifier = Modifier.weight(1f))
        Input(state)
    }
}

@Composable
private fun Header(state: ChatState) {
    Row(Modifier.fillMaxWidth().padding(8.dp)) {
        Text("Out: ${state.outputTokens}  In: ${state.inputTokens}")
    }
}

@Composable
private fun Messages(state: ChatState, modifier: Modifier = Modifier) {
    val scroll = rememberScrollState()
    Column(modifier.fillMaxWidth().verticalScroll(scroll).padding(8.dp)) {
        state.messages.forEach { msg ->
            Message(msg)
            Spacer(Modifier.Companion.height(4.dp))
        }
    }
}

@Composable
private fun Message(message: ChatMessage) {
    when (message) {
        is UserMessage -> Text(message.singleText())
        is AiMessage -> MarkdownText(message.text())
    }
}

@Composable
private fun Input(state: ChatState) {
    val text = rememberTextFieldState()
    fun sendMessage() {
        if (text.text.isNotBlank()) {
            state.onSendMessage(text.text.toString())
            text.clearText()
        }
    }
    Row(Modifier.Companion.fillMaxWidth().padding(8.dp)) {
        TextField(
            text,
            modifier = Modifier.Companion
                .weight(1f)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.Companion.KeyUp && event.key == Key.Companion.Enter) {
                        sendMessage()
                        true
                    } else {
                        false
                    }
                }
        )
        Spacer(Modifier.Companion.width(8.dp))
        ActionButton(
            onClick = { sendMessage() },
            enabled = !state.isSending
        ) { Text("Send") }
    }
}

@Composable
private fun ChatListScreen(state: ChatListState) {
    val scroll = rememberScrollState()
    Column(Modifier.fillMaxWidth().verticalScroll(scroll).padding(8.dp)) {
        state.chats.forEach { chat ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                ActionButton(onClick = { state.onOpenChat(chat.id) }) {
                    Column(Modifier.weight(1f)) {
                        Text(chat.firstMessage)
                        val date = SimpleDateFormat("yyyy-MM-dd HH:mm").format(Date(chat.createdAt))
                        Text(date)
                    }
                }
                Spacer(Modifier.width(8.dp))
                ActionButton(onClick = { state.onDeleteChat(chat.id) }) { Text("\uD83D\uDDD1") }
            }
        }
    }
}
@Composable
private fun RolesScreen(state: RolesState) {
    val textState = rememberTextFieldState(state.text)
    Column(Modifier.fillMaxSize().padding(8.dp)) {
        TextArea(
            textState,
            Modifier.weight(1f).fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        ActionButton(onClick = { state.onSave(textState.text.toString()) }, modifier = Modifier.fillMaxWidth()) {
            Text("Save")
        }
    }
}
