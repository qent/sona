package io.qent.sona.ui.chat

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.openapi.util.IconLoader
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.model.rememberMarkdownState
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.UserMessage
import io.qent.sona.PluginStateFlow
import io.qent.sona.core.State.ChatState
import org.jetbrains.jewel.ui.component.ActionButton
import org.jetbrains.jewel.ui.component.Text
import java.awt.image.BufferedImage

import io.qent.sona.ui.SonaTheme
import javax.swing.Icon

@Composable
fun ChatPanel(state: ChatState) {
    Column(
        Modifier
            .fillMaxSize()
            .background(SonaTheme.colors.Background)
    ) {
        ChatHeader(state)
        Box(
            Modifier.weight(1f)
        ) {
            Messages(state)
        }
        ChatInput(state)
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
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                if (message is AiMessage) {
                    val bottom: (@Composable () -> Unit)? = if (state.toolRequest && index == state.messages.lastIndex) {
                        @Composable { ToolPermissionButtons(state.onAllowTool, state.onAlwaysAllowTool, state.onDenyTool) }
                    } else null
                    MessageBubble(message, isUser = false, bottomContent = bottom, onDelete = { state.onDeleteFrom(index) })
                } else if (message is UserMessage) {
                    MessageBubble(message, isUser = true, onDelete = { state.onDeleteFrom(index) })
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MessageBubble(message: Any, isUser: Boolean, bottomContent: (@Composable () -> Unit)? = null, onDelete: () -> Unit) {
    if (message is AiMessage) {
        if (message.text().isNullOrEmpty()) return
    }

    var hovered by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val background = if (isUser) SonaTheme.colors.UserBubble else SonaTheme.colors.Background
    val textColor = if (isUser) SonaTheme.colors.UserText else SonaTheme.colors.AiText
    val messageText = when (message) {
        is AiMessage -> message.text().orEmpty()
        is UserMessage -> message.singleText().trim()
        else -> ""
    }
    Column(
        Modifier
            .pointerMoveFilter(
                onEnter = {
                    hovered = true
                    false
                },
                onExit = {
                    hovered = false
                    false
                }
            )
            .fillMaxWidth()
    ) {
        Box(
            Modifier
                .padding(top = if (isUser) 6.dp else 0.dp)
                .shadow(if (isUser) if (hovered) 6.dp else 2.dp else 0.dp, RoundedCornerShape(14.dp))
                .background(background, RoundedCornerShape(6.dp))
                .padding(horizontal = if (isUser) 12.dp else 0.dp, vertical = if (isUser) 8.dp else 2.dp)
                .fillMaxWidth()
        ) {
            SelectionContainer {
                Column(Modifier.fillMaxWidth()) {
                    if (message is AiMessage) {
                        val mdState = rememberMarkdownState(message.text(), immediate = true)
                        Markdown(
                            mdState,
                            colors = SonaTheme.markdownColors,
                            typography = SonaTheme.markdownTypography,
                            components = markdownComponents(
                                codeFence = { CopyableCodeBlock(it, true) },
                                codeBlock = { CopyableCodeBlock(it, false) },
                            ),
                        )
                    } else if (message is UserMessage) {
                        Text(
                            message.singleText().trim(),
                            color = textColor,
                            fontSize = 15.sp
                        )
                    }
                    bottomContent?.let {
                        Spacer(Modifier.height(8.dp))
                        it()
                    }
                }
            }
        }
        val alpha by animateFloatAsState(if (hovered) 1f else 0f)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (isUser) 4.dp else 0.dp)
                .alpha(alpha),
            horizontalArrangement = Arrangement.End,
        ) {
            Image(
                painter = loadIcon("/icons/copy.svg"),
                contentDescription = "Copy message",
                colorFilter = ColorFilter.tint(textColor),
                modifier = Modifier
                    .size(12.dp)
                    .clickable { clipboard.setText(AnnotatedString(messageText)) }
            )
            Spacer(Modifier.width(8.dp))
            Image(
                painter = loadIcon("/icons/trash.svg"),
                contentDescription = "Delete message",
                colorFilter = ColorFilter.tint(textColor),
                modifier = Modifier
                    .size(12.dp)
                    .clickable(onClick = onDelete)
            )
        }
    }
}

@Composable
private fun ToolPermissionButtons(
    onOk: () -> Unit,
    onAlways: () -> Unit,
    onCancel: () -> Unit,
) {
    Box(
        modifier = Modifier
            .padding(top = 2.dp)
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
            .padding(2.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            ActionButton(onClick = onOk, modifier = Modifier.weight(1f)) {
                Text("OK", fontWeight = FontWeight.Bold)
            }
            ActionButton(onClick = onAlways, modifier = Modifier.weight(2f)) {
                Text("Always in this chat")
            }
            ActionButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
        }
    }
}

@Composable
fun loadIcon(path: String): Painter {
    val icon = IconLoader.getIcon(path, PluginStateFlow::class.java)
    val bufferedImage = iconToImage(icon)
    return BitmapPainter(bufferedImage.toComposeImageBitmap())
}

fun iconToImage(icon: Icon): BufferedImage {
    val image = BufferedImage(icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    icon.paintIcon(null, g, 0, 0)
    g.dispose()
    return image
}
