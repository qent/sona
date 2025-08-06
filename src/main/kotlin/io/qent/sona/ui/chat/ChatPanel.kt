package io.qent.sona.ui.chat

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.project.Project
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.model.rememberMarkdownState
import io.qent.sona.core.State.ChatState
import io.qent.sona.core.UiMessage
import io.qent.sona.PluginStateFlow
import org.jetbrains.jewel.ui.component.ActionButton
import org.jetbrains.jewel.ui.component.Text
import java.awt.image.BufferedImage

import io.qent.sona.ui.SonaTheme
import dev.langchain4j.agent.tool.ToolExecutionRequest
import kotlinx.coroutines.delay
import javax.swing.Icon

@Composable
fun ChatPanel(project: Project, state: ChatState) {
    Column(
        Modifier
            .fillMaxSize()
            .background(SonaTheme.colors.Background)
    ) {
        ChatHeader(state)
        Box(
            Modifier.weight(1f)
        ) {
            Messages(project, state)
        }
        ChatInput(state)
    }
}

@Composable
private fun Messages(project: Project, state: ChatState, modifier: Modifier = Modifier) {
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
                val bottom: (@Composable () -> Unit)? = if (
                    message is UiMessage.Ai &&
                    state.toolRequest &&
                    index == state.messages.lastIndex
                ) {
                    @Composable { ToolPermissionButtons(state.onAllowTool, state.onAlwaysAllowTool, state.onDenyTool) }
                } else null

                if (message is UiMessage.Ai || message is UiMessage.User) {
                    MessageBubble(project, message, bottomContent = bottom, onDelete = { state.onDeleteFrom(index) })
                } else if (message is UiMessage.Tool) {
                    ToolMessageBubble(message, onDelete = { state.onDeleteFrom(index) })
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
fun MessageBubble(
    project: Project, message: UiMessage,
    bottomContent: (@Composable () -> Unit)? = null,
    onDelete: () -> Unit,
) {
    if (message is UiMessage.Ai && message.text.isEmpty()) return

    val isUser = message is UiMessage.User
    var hovered by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val background = if (isUser) SonaTheme.colors.UserBubble else SonaTheme.colors.Background
    val textColor = if (isUser) SonaTheme.colors.UserText else SonaTheme.colors.AiText
    val messageText = message.text
    var showTools by remember { mutableStateOf(false) }
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
                    if (message is UiMessage.Ai) {
                        val mdState = rememberMarkdownState(message.text, immediate = true)
                        Markdown(
                            mdState,
                            colors = SonaTheme.markdownColors,
                            typography = SonaTheme.markdownTypography,
                            components = markdownComponents(
                                codeFence = { CopyableCodeBlock(project, it, true) },
                                codeBlock = { CopyableCodeBlock(project, it, false) },
                            ),
                        )
                        if (showTools && message.toolRequests.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            ToolRequests(message.toolRequests)
                        }
                    } else if (message is UiMessage.User) {
                        Text(
                            message.text,
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
            if (message is UiMessage.Ai && message.toolRequests.isNotEmpty()) {
                Image(
                    painter = loadIcon("/icons/gear.svg"),
                    contentDescription = "Show tool requests",
                    colorFilter = ColorFilter.tint(textColor),
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .clickable { showTools = !showTools }
                )
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ToolMessageBubble(
    message: UiMessage.Tool,
    onDelete: () -> Unit,
) {
    var hovered by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val background = SonaTheme.colors.AiBubble
    val textColor = SonaTheme.colors.AiText
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
                .padding(top = 6.dp)
                .shadow(2.dp, RoundedCornerShape(14.dp))
                .background(background, RoundedCornerShape(6.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .height(if (message.text == "executing") 20.dp else 160.dp)
                .fillMaxWidth()
        ) {
            if (message.text == "executing") {
                AnimatedDots(textColor)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    SelectionContainer {
                        Text(
                            message.text,
                            color = textColor,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
        val alpha by animateFloatAsState(if (hovered) 1f else 0f)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .alpha(alpha),
            horizontalArrangement = Arrangement.End,
        ) {
            Image(
                painter = loadIcon("/icons/copy.svg"),
                contentDescription = "Copy message",
                colorFilter = ColorFilter.tint(textColor),
                modifier = Modifier
                    .size(12.dp)
                    .clickable { clipboard.setText(AnnotatedString(message.text)) }
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
private fun AnimatedDots(color: Color) {
    var dots by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            dots = (dots + 1) % 4
            delay(300)
        }
    }
    Text(".".repeat(dots), color = color, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
}

@Composable
private fun ToolRequests(requests: List<ToolExecutionRequest>) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(SonaTheme.colors.Background.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(4.dp)
    ) {
        requests.forEach { req ->
            Text("${req.name()}: ${req.arguments()}", color = SonaTheme.colors.AiText, fontSize = 13.sp)
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
