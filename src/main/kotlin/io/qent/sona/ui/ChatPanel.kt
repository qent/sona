
package io.qent.sona.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.model.rememberMarkdownState
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.UserMessage
import io.qent.sona.core.State.ChatState
import org.jetbrains.jewel.ui.component.ActionButton
import org.jetbrains.jewel.ui.component.Text

object ChatColors {
    val Background = Color(0xFF20232A)
    val UserBubble = Color(0xFF3366FF)
    val AiBubble = Color(0xFF292D36)
    val UserText = Color.White
    val AiText = Color(0xFFD3D8DF)
    val InputBackground = Color(0xFF22262A)
    val Placeholder = Color(0xFF7A818A)
    val BubbleShadow = Color(0x22000000)
    val BorderFocused = Color(0xFF3B72FF)
    val BorderDefault = Color(0xFF373B42)
}

@Composable
fun ChatPanel(state: ChatState) {
    Column(
        Modifier
            .fillMaxSize()
            .background(ChatColors.Background)
    ) {
        Header(state)
        Box(
            Modifier.weight(1f).padding(vertical = 8.dp)
        ) {
            Messages(state)
        }
        Input(state)
    }
}

@Composable
private fun Header(state: ChatState) {
    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
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
            .padding(horizontal = 16.dp)
    ) {
        items(state.messages.size) { index ->
            val message = state.messages[index]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = if (message is UserMessage) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.Bottom
            ) {
                if (message is AiMessage) {
                    AiAvatar()
                    Spacer(Modifier.width(8.dp))
                    MessageBubble(message, isUser = false)
                } else if (message is UserMessage) {
                    MessageBubble(message, isUser = true)
                }
            }
            Spacer(Modifier.height(2.dp))
        }
    }
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }
}

@Composable
fun MessageBubble(message: Any, isUser: Boolean) {
    var hovered by remember { mutableStateOf(false) }
    val background = if (isUser) ChatColors.UserBubble else ChatColors.AiBubble
    val textColor = if (isUser) ChatColors.UserText else ChatColors.AiText
    Box(
        Modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { hovered = true; tryAwaitRelease(); hovered = false }
                )
            }
            .shadow(if (hovered) 6.dp else 2.dp, RoundedCornerShape(14.dp))
            .background(background, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .widthIn(max = 420.dp)
    ) {
        if (message is AiMessage) {
            val mdState = rememberMarkdownState(message.text(), immediate = true)
            Markdown(
                mdState,
                colors = Colors.Dark,
                typography = Typography.Dark,
            )
        } else if (message is UserMessage) {
            Text(
                message.singleText(),
                color = textColor,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun AiAvatar() {
    Box(
        Modifier
            .size(28.dp)
            .background(Color(0xFF8E98A9), shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("🤖", fontSize = 16.sp)
    }
}

@Composable
private fun Input(state: ChatState) {
    val text = remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxWidth()
            .background(ChatColors.InputBackground)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .weight(1f)
                    .background(ChatColors.Background, RoundedCornerShape(12.dp))
                    .border(
                        1.dp,
                        if (isFocused) ChatColors.BorderFocused else ChatColors.BorderDefault,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (text.value.isEmpty()) {
                    Text(
                        "Напишите сообщение...",
                        color = ChatColors.Placeholder,
                        fontSize = 14.sp
                    )
                }
                BasicTextField(
                    value = text.value,
                    onValueChange = { text.value = it },
                    enabled = !state.isSending,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused }
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.Companion.KeyUp && event.key == Key.Companion.Enter) {
                                if (text.value.isNotBlank()) {
                                    state.onSendMessage(text.value)
                                    text.value = ""
                                }
                                true
                            } else {
                                false
                            }
                        },
                    cursorBrush = SolidColor(Color.White),
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 14.sp
                    ),
                    singleLine = true,
                )
            }
            Spacer(Modifier.width(8.dp))
            ActionButton(
                onClick = {
                    if (text.value.isNotBlank()) {
                        state.onSendMessage(text.value)
                        text.value = ""
                    }
                },
                enabled = text.value.isNotBlank() && !state.isSending,
                modifier = Modifier.height(40.dp)
            ) {
                Text("➤")
            }
        }
    }
}
