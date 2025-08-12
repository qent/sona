package io.qent.sona.ui.chat

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.runtime.MutableState
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.openapi.project.Project
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.model.rememberMarkdownState
import io.qent.sona.Strings
import io.qent.sona.core.state.State.ChatState
import io.qent.sona.core.state.UiMessage
import io.qent.sona.ui.common.loadIcon
import org.jetbrains.jewel.ui.component.ActionButton
import org.jetbrains.jewel.ui.component.Text
import io.qent.sona.ui.common.SonaTheme
import dev.langchain4j.agent.tool.ToolExecutionRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChatPanel(project: Project, state: ChatState) {
    val inputText = remember { mutableStateOf(TextFieldValue("")) }
    val focusRequester = remember { FocusRequester() }
    Column(
        Modifier
            .fillMaxSize()
            .background(SonaTheme.colors.Background)
    ) {
        ChatHeader(state)
        Box(
            Modifier.weight(1f)
        ) {
            Messages(project, state, inputText, focusRequester)
        }
        ChatInput(state, inputText, focusRequester)
    }
}

@Composable
private fun Messages(
    project: Project,
    state: ChatState,
    inputText: MutableState<TextFieldValue>,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
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
                    MessageBubble(
                        project,
                        index,
                        message,
                        bottomContent = bottom,
                        onDelete = { state.onDeleteFrom(index) },
                        onEdit = {
                            inputText.value = TextFieldValue(message.text)
                            focusRequester.requestFocus()
                            state.onDeleteFrom(index)
                        },
                        onScrollOutside = { delta ->
                            coroutineScope.launch {
                                listState.scrollBy(delta)
                            }
                        }
                    )
                } else if (message is UiMessage.AiMessageWithTools) {
                    AiWithToolsMessageBubble(message)
                }
            }
            Spacer(Modifier.height(2.dp))
        }
    }
    LaunchedEffect(state.messages.lastOrNull()) {
        if (state.messages.isNotEmpty()) {
            listState.scrollToItem(state.messages.lastIndex)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MessageBubble(
    project: Project,
    key: Any,
    message: UiMessage,
    bottomContent: (@Composable () -> Unit)? = null,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onScrollOutside: (Float) -> Unit,
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
                                codeFence = { CopyableCodeBlock(project, it, key, true, onScrollOutside = onScrollOutside) },
                                codeBlock = { CopyableCodeBlock(project, it, key, false, onScrollOutside = onScrollOutside) },
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
                    contentDescription = Strings.showToolRequests,
                    colorFilter = ColorFilter.tint(textColor),
                    modifier = Modifier
                        .size(24.dp)
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
                contentDescription = Strings.copyMessage,
                colorFilter = ColorFilter.tint(textColor),
                modifier = Modifier
                    .size(12.dp)
                    .clickable { clipboard.setText(AnnotatedString(messageText)) }
            )
            if (isUser) {
                Spacer(Modifier.width(8.dp))
                Image(
                    painter = loadIcon("/icons/edit.svg"),
                    contentDescription = Strings.editMessage,
                    colorFilter = ColorFilter.tint(textColor),
                    modifier = Modifier
                        .size(12.dp)
                        .clickable(onClick = onEdit)
                )
                Spacer(Modifier.width(8.dp))
                Image(
                    painter = loadIcon("/icons/trash.svg"),
                    contentDescription = Strings.deleteMessage,
                    colorFilter = ColorFilter.tint(textColor),
                    modifier = Modifier
                        .size(12.dp)
                        .clickable(onClick = onDelete)
                )
            }
        }
    }
}

@Composable
fun AiWithToolsMessageBubble(
    message: UiMessage.AiMessageWithTools,
) {
    var expanded by remember { mutableStateOf(false) }
    val background = SonaTheme.colors.Background
    val textColor = SonaTheme.colors.AiText
    val borderColor = Color.White.copy(alpha = 0.12f)

    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .background(background, RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            // Header row: main text + toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                SelectionContainer(Modifier.weight(1f)) {
                    Text(
                        message.text,
                        color = textColor,
                        fontSize = 15.sp
                    )
                }
                TriangleToggle(expanded = expanded, tint = textColor)
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))

                // Tool requests (kept subtle, inside container)
                if (message.toolRequests.isNotEmpty()) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(SonaTheme.colors.Background.copy(alpha = 0.06f), RoundedCornerShape(4.dp))
                            .padding(6.dp)
                    ) {
                        ToolRequests(message.toolRequests)
                    }
                }

                // Divider between requests and responses (if both exist)
                if (message.toolRequests.isNotEmpty() && message.toolResponse.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(borderColor.copy(alpha = 0.25f))
                    )
                    Spacer(Modifier.height(6.dp))
                } else if (message.toolRequests.isNotEmpty() || message.toolResponse.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                }

                // Tool responses (monospace, scroll-safe, inside container)
                if (message.toolResponse.isNotEmpty()) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(SonaTheme.colors.Background.copy(alpha = 0.06f), RoundedCornerShape(4.dp))
                            .padding(6.dp)
                    ) {
                        message.toolResponse.forEach { line ->
                            SelectionContainer {
                                Text(
                                    line,
                                    color = textColor,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TriangleToggle(expanded: Boolean, tint: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(16.dp)
            .padding(start = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize().rotate(if (expanded) 90f else 0f)) {
            val path = androidx.compose.ui.graphics.Path().apply {
                // Draw a right-pointing triangle inside the box
                moveTo(size.width * 0.30f, size.height * 0.20f)
                lineTo(size.width * 0.30f, size.height * 0.80f)
                lineTo(size.width * 0.75f, size.height * 0.50f)
                close()
            }
            drawPath(path, color = tint)
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
                  Text(Strings.ok, fontWeight = FontWeight.Bold)
              }
              ActionButton(onClick = onAlways, modifier = Modifier.weight(2f)) {
                  Text(Strings.alwaysInThisChat)
              }
              ActionButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                  Text(Strings.cancel)
              }
        }
    }
}
