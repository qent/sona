import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.qent.sona.core.State.ChatListState
import org.jetbrains.jewel.ui.component.ActionButton
import org.jetbrains.jewel.ui.component.Text
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun ChatListPanel(state: ChatListState) {
    val listState = rememberLazyListState()
    Column(Modifier.fillMaxSize().padding(8.dp)) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(state.chats.size) { idx ->
                val chat = state.chats[idx]
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = { state.onOpenChat(chat.id) })
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Column(
                        Modifier.weight(1f)
                    ) {
                        Text(chat.firstMessage, maxLines = 1)
                        val date = SimpleDateFormat("yyyy-MM-dd HH:mm").format(Date(chat.createdAt))
                        Text(date, fontSize = 12.sp)
                    }
                    ActionButton(
                        onClick = {
                            state.onDeleteChat(chat.id)
                        },
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .align(androidx.compose.ui.Alignment.CenterVertically)
                    ) {
                        Text("\uD83D\uDDD1")
                    }
                }
            }
        }
    }
}