package io.qent.sona.ui.chat
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.openapi.project.Project
import com.intellij.openapi.components.service
import io.qent.sona.Strings
import io.qent.sona.core.state.State.ChatState
import io.qent.sona.core.state.UiMessage
import io.qent.sona.ui.common.SonaTheme
import io.qent.sona.services.PatchService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.jewel.ui.component.ActionButton
import org.jetbrains.jewel.ui.component.Text

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
            .padding(horizontal = 8.dp)
    ) {
        itemsIndexed(
            state.messages,
            key = { _, message -> message.hashCode() }
        ) { index, message ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                val bottom: (@Composable () -> Unit)? = if (
                    message is UiMessage.Ai &&
                    state.toolRequest != null &&
                    index == state.messages.lastIndex
                ) {
                    if (state.toolRequest == "applyPatch") {
                        @Composable {
                            PatchPermissionButtons(
                                onApply = state.onAllowTool,
                                onViewDiff = {
                                    state.pendingPatch?.let { project.service<PatchService>().showPatchDiff(it) }
                                },
                                onCancel = state.onDenyTool
                            )
                        }
                    } else {
                        @Composable { ToolPermissionButtons(state.onAllowTool, state.onAlwaysAllowTool, state.onDenyTool) }
                    }
                } else null

                if (message is UiMessage.Ai || message is UiMessage.User) {
                    val messageId = message.hashCode()
                    MessageBubble(
                        project,
                        messageId,
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

            if (index == state.messages.size - 1) {
                Spacer(Modifier.height(60.dp))
            } else {
                Spacer(Modifier.height(2.dp))
            }
        }
    }
    LaunchedEffect(state.messages.lastOrNull()) {
        if (state.messages.isNotEmpty()) {
            listState.scrollToItem(state.messages.lastIndex)
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

@Composable
private fun PatchPermissionButtons(
    onApply: () -> Unit,
    onViewDiff: () -> Unit,
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
            ActionButton(onClick = onApply, modifier = Modifier.weight(1f)) {
                Text(Strings.applyPatch, fontWeight = FontWeight.Bold)
            }
            ActionButton(onClick = onViewDiff, modifier = Modifier.weight(2f)) {
                Text(Strings.viewDiff)
            }
            ActionButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text(Strings.cancel)
            }
        }
    }
}
