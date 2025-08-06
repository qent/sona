import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.qent.sona.core.State.ChatListState
import io.qent.sona.ui.SonaTheme
import org.jetbrains.jewel.ui.component.ActionButton
import org.jetbrains.jewel.ui.component.Text
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatListPanel(state: ChatListState) {
    val listState = rememberLazyListState()
    Column(
        Modifier
            .fillMaxSize()
            .background(SonaTheme.colors.Background)
            .padding(8.dp)
    ) {
        Text(
            "History",
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
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SonaTheme.colors.AiBubble)
                        .clickable(onClick = { state.onOpenChat(chat.id) })
                ) {
                    ActionButton(
                        onClick = { state.onDeleteChat(chat.id) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(horizontal = 6.dp)
                            .padding(top = 8.dp)
                    ) {
                        Text("\uD83D\uDDD1")
                    }

                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(chat.firstMessage, maxLines = 1, color = SonaTheme.colors.AiText)
                        Spacer(modifier = Modifier.height(6.dp))
                        val date = SimpleDateFormat("yyyy-MM-dd HH:mm").format(Date(chat.createdAt))
                        Text(
                            text = "$date · ${chat.messages} messages",
                            fontSize = 12.sp,
                            color = SonaTheme.colors.Placeholder
                        )
                    }
                }
            }
        }
    }
}