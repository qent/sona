package io.qent.sona.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.model.rememberMarkdownState
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.UserMessage
import io.qent.sona.core.State.ChatState
import org.jetbrains.jewel.ui.component.ActionButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField


@Composable
fun ChatPanel(state: ChatState) {
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
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        items(state.messages.size) { index ->
            val message = state.messages[index]
            when (message) {
                is UserMessage -> Text(message.singleText())
                is AiMessage -> {
                    val state = rememberMarkdownState(message.text(), immediate = true)
                    Markdown(
                        state,
                        colors = Colors.Dark,
                        typography = Typography.Dark,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
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
