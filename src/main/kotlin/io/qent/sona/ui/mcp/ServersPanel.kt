package io.qent.sona.ui.mcp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.qent.sona.core.McpServerStatus
import io.qent.sona.core.State
import io.qent.sona.ui.SonaTheme
import org.jetbrains.jewel.ui.component.ActionButton
import org.jetbrains.jewel.ui.component.Text
import java.awt.Desktop
import java.net.URI

@Composable
fun ServersPanel(state: State.ServersState) {
    val servers by state.servers.collectAsState(emptyList())
    val listState = rememberLazyListState()
    Column(
        Modifier
            .fillMaxSize()
            .background(SonaTheme.colors.Background)
            .padding(8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            ActionButton(onClick = state.onReload) {
                Text("\u27f3", Modifier.padding(2.dp), fontSize = 24.sp)
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(servers) { server ->
                val expanded = remember { mutableStateOf(false) }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SonaTheme.colors.AiBubble)
                        .clickable {
                            if (server.status is McpServerStatus.Status.CONNECTED) {
                                expanded.value = !expanded.value
                            }
                        }
                        .padding(4.dp)
                        .padding(start = 8.dp),
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(server.name, color = SonaTheme.colors.AiText, modifier = Modifier.weight(1f))
                        val color = when (server.status) {
                            is McpServerStatus.Status.DISABLED -> Color.Gray
                            is McpServerStatus.Status.FAILED -> Color(0xFFF44336)
                            is McpServerStatus.Status.CONNECTING -> Color(0xFFFFC107)
                            is McpServerStatus.Status.CONNECTED -> Color(0xFF4CAF50)
                        }
                        Box(
                            Modifier
                                .clickable { state.onToggleServer(server.name) }
                                .padding(8.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }

                    val status = server.status
                    when (status) {
                        is McpServerStatus.Status.FAILED -> {
                            Text(status.e.toString(), Modifier.padding(vertical = 8.dp), color = SonaTheme.colors.BackgroundText)
                            if (server.jetbrainsMcpProxyUnavailable()) {
                                Box(
                                    modifier = Modifier
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(color = SonaTheme.colors.UserBubble)
                                        .clickable {
                                            Desktop.getDesktop().browse(URI("https://plugins.jetbrains.com/plugin/26071-mcp-server"))
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        "Install JetBrains MCP Server Plugin",
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                        is McpServerStatus.Status.CONNECTED -> {
                            if (expanded.value) {
                                for (tool in server.tools) {
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(top = 12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(tool.name(), Modifier.width(200.dp), fontWeight = FontWeight.Bold)
                                        Text(
                                            tool.description().trimIndent(),
                                            color = SonaTheme.colors.BackgroundText
                                        )
                                    }
                                }
                            }
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}

fun McpServerStatus.jetbrainsMcpProxyUnavailable(

) = name == "@jetbrains/mcp-proxy" && (status as? McpServerStatus.Status.FAILED)?.e?.toString()?.let { error ->
    error.contains("NullPointerException") && error.contains("com.fasterxml.jackson.databind.JsonNode")
}  ?: false
