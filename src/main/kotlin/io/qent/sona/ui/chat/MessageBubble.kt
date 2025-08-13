package io.qent.sona.ui.chat

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.openapi.project.Project
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.model.rememberMarkdownState
import io.qent.sona.Strings
import io.qent.sona.core.state.UiMessage
import io.qent.sona.ui.common.LocalTypography
import io.qent.sona.ui.common.SonaTheme
import io.qent.sona.ui.common.loadIcon
import org.jetbrains.jewel.ui.component.Text

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
    val borderColor by remember { mutableStateOf(Color.White.copy(alpha = 0.12f)) }

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
        val toolRequested = message is UiMessage.Ai && message.toolRequests.isNotEmpty()

        Row(
            Modifier
                .padding(top = if (isUser) 6.dp else 0.dp)
                .padding(start = if (isUser) 0.dp else 8.dp, end = if (isUser || toolRequested) 0.dp else 8.dp)
                .background(background, RoundedCornerShape(6.dp))
                .padding(vertical = if (isUser) 8.dp else 2.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            SelectionContainer(Modifier.weight(1f)) {
                Column(
                    Modifier.fillMaxWidth()
                        .padding(horizontal = if (isUser) 8.dp else 0.dp)
                ) {
                    if (message is UiMessage.Ai) {
                        val mdState = rememberMarkdownState(message.text, immediate = true)
                        Markdown(
                            mdState,
                            colors = SonaTheme.markdownColors,
                            typography = SonaTheme.markdownTypography,
                            components = markdownComponents(
                                codeFence = {
                                    CopyableCodeBlock(
                                        project,
                                        it,
                                        key,
                                        true,
                                        onScrollOutside = onScrollOutside
                                    )
                                },
                                codeBlock = {
                                    CopyableCodeBlock(
                                        project,
                                        it,
                                        key,
                                        false,
                                        onScrollOutside = onScrollOutside
                                    )
                                },
                            ),
                        )
                    } else if (message is UiMessage.User) {
                        Text(
                            message.text,
                            color = textColor,
                            fontSize = 15.sp
                        )
                    }
                }
            }
            if (message is UiMessage.Ai && message.toolRequests.isNotEmpty()) {
                Image(
                    painter = loadIcon("/icons/info.svg"),
                    contentDescription = Strings.showToolRequests,
                    colorFilter = ColorFilter.tint(LocalTypography.current.text.color),
                    modifier = Modifier
                        .size(14.dp)
                        .clickable { showTools = !showTools }
                )
            }
        }
        if (showTools && message is UiMessage.Ai && message.toolRequests.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                    .background(background, RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                ToolRequests(message.toolRequests)
            }
        }

        bottomContent?.let {
            Spacer(Modifier.height(8.dp))
            it()
            Spacer(Modifier.height(4.dp))
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