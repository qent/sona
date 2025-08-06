package io.qent.sona.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.qent.sona.core.McpServerStatus
import io.qent.sona.core.State
import org.jetbrains.jewel.ui.component.ActionButton
import org.jetbrains.jewel.ui.component.Text

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
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SonaTheme.colors.AiBubble)
                        .clickable { state.onToggleServer(server.name) }
                        .padding(vertical = 8.dp, horizontal = 12.dp),
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
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
        }
    }
}
