package io.qent.sona.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.qent.sona.core.McpServerStatus
import io.qent.sona.core.State
import org.jetbrains.jewel.ui.component.Text

@Composable
fun ServersPanel(state: State.ServersState) {
    val servers by state.servers.collectAsState(emptyList())
    Column(
        Modifier
            .fillMaxSize()
            .background(SonaTheme.colors.Background)
            .padding(8.dp)
    ) {
        LazyColumn(Modifier.fillMaxSize()) {
            items(servers) { server ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(server.name, color = SonaTheme.colors.AiText, modifier = Modifier.weight(1f))
                    val statusText = when (server.status) {
                        is McpServerStatus.Status.CONNECTING -> "Connecting"
                        is McpServerStatus.Status.CONNECTED -> "Connected"
                        is McpServerStatus.Status.FAILED -> "Failed"
                    }
                    Text(statusText, color = SonaTheme.colors.AiText)
                }
            }
        }
    }
}
