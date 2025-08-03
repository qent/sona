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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.qent.sona.core.DefaultRoles
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.model.rememberMarkdownState
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.UserMessage
import io.qent.sona.core.State.ChatState
import org.jetbrains.jewel.ui.component.ActionButton
import org.jetbrains.jewel.ui.component.Text

@Composable
fun ChatPanel(state: ChatState) {
    Column(
        Modifier
            .fillMaxSize()
            .background(SonaTheme.colors.Background)
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
            .padding(horizontal = 10.dp)
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
    val background = if (isUser) SonaTheme.colors.UserBubble else SonaTheme.colors.AiBubble
    val textColor = if (isUser) SonaTheme.colors.UserText else SonaTheme.colors.AiText
    Box(
        Modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { hovered = true; tryAwaitRelease(); hovered = false }
                )
            }
            .shadow(if (hovered) 6.dp else 2.dp, RoundedCornerShape(14.dp))
            .background(background, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .widthIn(max = 420.dp)
    ) {
        if (message is AiMessage) {
            val mdState = rememberMarkdownState(message.text(), immediate = true)
            Markdown(
                mdState,
                colors = SonaTheme.markdownColors,
                typography = SonaTheme.markdownTypography,
            )
        } else if (message is UserMessage) {
            Text(
                message.singleText().trim(),
                color = textColor,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun Input(state: ChatState) {
    val text = remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
    val placeholder = if (state.messages.isNotEmpty() || state.roles.isEmpty()) {
        "Type a message..."
    } else {
        when (state.roles[state.activeRole]) {
            DefaultRoles.ARCHITECT -> "Describe what you'd like to plan and design..."
            DefaultRoles.CODE -> "Describe what you'd like to implement..."
            else -> "Describe your task..."
        }
    }


    Box(
        Modifier
            .fillMaxWidth()
            .background(SonaTheme.colors.InputBackground)
            .padding(10.dp, 8.dp)
    ) {
        Row(
            modifier = Modifier.align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .background(SonaTheme.colors.Background, RoundedCornerShape(12.dp))
                    .border(
                        1.dp,
                        if (isFocused) SonaTheme.colors.BorderFocused else SonaTheme.colors.BorderDefault,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (text.value.isEmpty()) {
                    Text(
                        placeholder,
                        color = SonaTheme.colors.Placeholder,
                        fontSize = 14.sp
                    )
                }
                BasicTextField(
                    value = text.value,
                    onValueChange = { text.value = it },
                    enabled = !state.isSending,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
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
                        color = SonaTheme.colors.UserText,
                        fontSize = 14.sp
                    ),
                    singleLine = false,
                )
            }
        }

        DropdownSelector(
            items = state.roles,
            selectedIndex = state.activeRole,
            expandUpwards = true,
            onSelect = { state.onSelectRole(it) },
            backgroundColor = Color.White.copy(alpha = 0.2f),
            modifier = Modifier.align(Alignment.BottomStart).padding(6.dp).alpha(0.6f)
        )

        DropdownSelector(
            items = state.presets.presets.map { it.name },
            selectedIndex = state.presets.active,
            expandUpwards = true,
            onSelect = { state.onSelectPreset(it) },
            backgroundColor = Color.White.copy(alpha = 0.2f),
            modifier = Modifier.align(Alignment.BottomStart).padding(6.dp).offset(x = 115.dp).alpha(0.6f)
        )

        if (state.isSending) {
            ActionButton(
                onClick = { state.onStop() },
                modifier = Modifier.height(40.dp).width(40.dp).align(Alignment.BottomEnd)
            ) {
                Text("■")
            }
        } else {
            ActionButton(
                onClick = {
                    if (text.value.isNotBlank()) {
                        state.onSendMessage(text.value)
                        text.value = ""
                    }
                },
                enabled = text.value.isNotBlank(),
                modifier = Modifier
                    .padding(4.dp)
                    .height(30.dp)
                    .width(30.dp)
                    .clip(CircleShape)
                    .alpha(alpha = if (text.value.isBlank()) 0.4f else 1f)
                    .align(Alignment.BottomEnd)
            ) {
                Text("➤")
            }
        }
    }
}
