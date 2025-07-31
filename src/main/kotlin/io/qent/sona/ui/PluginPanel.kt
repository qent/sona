package io.qent.sona.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import io.qent.sona.core.State
import io.qent.sona.core.StateProvider
import org.jetbrains.jewel.bridge.JewelComposePanel
import org.jetbrains.jewel.ui.component.ActionButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField

class PluginPanel(private val pluginStateProvider: StateProvider) {
    fun component() = JewelComposePanel { Ui() }

    @Composable
    private fun Ui() {
        val chatState = pluginStateProvider.state.collectAsState()
        Column(Modifier.Companion.fillMaxSize()) {
            Header(chatState.value)
            Messages(chatState.value, modifier = Modifier.Companion.weight(1f))
            Input(chatState.value)
        }
    }

    @Composable
    private fun Header(state: State) {
        Row(Modifier.Companion.fillMaxWidth().padding(8.dp)) {
            Text("Out: ${state.outgoingTokens}  In: ${state.incomingTokens}")
        }
    }

    @Composable
    private fun Messages(state: State, modifier: Modifier = Modifier.Companion) {
        val scroll = rememberScrollState()
        Column(modifier.fillMaxWidth().verticalScroll(scroll).padding(8.dp)) {
            state.messages.forEach { msg ->
                Text(msg.toString())
                Spacer(Modifier.Companion.height(4.dp))
            }
        }
    }

    @Composable
    private fun Input(state: State) {
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
}