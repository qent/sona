package io.qent.sona.ui.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.model.rememberMarkdownState
import dev.langchain4j.agent.tool.ToolExecutionRequest
import io.qent.sona.Strings
import io.qent.sona.core.state.UiMessage
import io.qent.sona.ui.common.LocalTypography
import io.qent.sona.ui.common.SonaTheme
import io.qent.sona.ui.common.loadIcon
import kotlinx.coroutines.delay
import org.jetbrains.jewel.ui.component.Text

@Composable
fun AiWithToolsMessageBubble(
    message: UiMessage.AiMessageWithTools,
) {
    var expanded by remember { mutableStateOf(false) }
    val background = SonaTheme.colors.Background
    val borderColor = Color.White.copy(alpha = 0.12f)
    val messageText = if (message.text.isEmpty() && message.toolRequests.isNotEmpty()) {
        val toolNames = message.toolRequests.joinToString(", ") { it.name() }
        String.format(Strings.toolCalling, toolNames)
    } else {
        message.text
    }

    Column(Modifier.fillMaxWidth()) {
        // Header row: main text + toggle
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            SelectionContainer(Modifier.weight(1f).padding(bottom = 12.dp).padding(horizontal = 8.dp)) {
                val mdState = rememberMarkdownState(messageText, immediate = true)
                Markdown(
                    mdState,
                    colors = SonaTheme.markdownColors,
                    typography = SonaTheme.markdownTypography
                )
            }

            Image(
                painter = loadIcon("/icons/info.svg"),
                contentDescription = Strings.showToolRequests,
                colorFilter = ColorFilter.tint(LocalTypography.current.text.color),
                modifier = Modifier
                    .size(14.dp)
                    .clickable { expanded = !expanded }
            )
        }

        if (expanded) {
            Spacer(Modifier.height(8.dp))
            Column (
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                    .background(background, RoundedCornerShape(6.dp))
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                // Tool requests (kept subtle, inside container)
                if (message.toolRequests.isNotEmpty()) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(SonaTheme.colors.Background.copy(alpha = 0.06f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        ToolRequests(message.toolRequests)
                    }
                }

                if (message.toolResponse.isEmpty()) {
                    AnimatedDots(SonaTheme.colors.AiText)
                }

                // Divider between requests and responses (if both exist)
                if (message.toolRequests.isNotEmpty() && message.toolResponse.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .padding(horizontal = 8.dp)
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
                            .padding(4.dp)
                    ) {
                        message.toolResponse.forEach { line ->
                            SelectionContainer(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                                Text(
                                    line,
                                    color = SonaTheme.colors.AiText,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun ToolRequests(requests: List<ToolExecutionRequest>) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(SonaTheme.colors.Background.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(4.dp)
    ) {
        requests.forEach { req ->
            SelectionContainer {
                Text("${req.name()}: ${req.arguments()}", color = SonaTheme.colors.AiText, fontSize = 13.sp)
            }
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
