import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.qent.sona.Strings
import io.qent.sona.core.state.State.ChatListState
import io.qent.sona.ui.SonaTheme
import io.qent.sona.ui.common.DeleteConfirmationDialog
import io.qent.sona.ui.common.TwoLineItem
import org.jetbrains.jewel.ui.component.Text
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatListPanel(state: ChatListState) {
    var deleteId by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    Column(
        Modifier
            .fillMaxSize()
            .background(SonaTheme.colors.Background)
            .padding(8.dp)
    ) {
        Text(
            Strings.history,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
            style = SonaTheme.markdownTypography.h5
        )
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(state.chats.size) { idx ->
                val chat = state.chats[idx]
                val date = SimpleDateFormat("yyyy-MM-dd HH:mm").format(Date(chat.createdAt))
                TwoLineItem(
                    title = chat.firstMessage,
                    subtitle = "$date · ${chat.messages} ${Strings.messages}",
                    selected = false,
                    onClick = { state.onOpenChat(chat.id) },
                    onDelete = { deleteId = chat.id }
                )
            }
        }
    }
    deleteId?.let { id ->
        DeleteConfirmationDialog(
            text = Strings.deleteChatQuestion,
            onConfirm = {
                state.onDeleteChat(id)
                deleteId = null
            },
            onCancel = { deleteId = null }
        )
    }
}