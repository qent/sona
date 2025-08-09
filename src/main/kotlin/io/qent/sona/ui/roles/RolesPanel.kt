package io.qent.sona.ui.roles

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import io.qent.sona.Strings
import io.qent.sona.core.roles.DefaultRoles
import io.qent.sona.core.roles.Role
import io.qent.sona.core.state.State
import io.qent.sona.ui.SonaTheme
import io.qent.sona.ui.common.DeleteConfirmationDialog
import org.jetbrains.jewel.ui.component.ActionButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextArea
import org.jetbrains.jewel.ui.component.TextField

@Composable
fun RolesPanel(state: State.RolesListState) {
    var deleteIndex by remember { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState()
    Box(
        Modifier
            .fillMaxSize()
            .background(SonaTheme.colors.Background)
            .padding(8.dp)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 56.dp)
        ) {
            itemsIndexed(state.roles) { idx, role ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (idx == state.currentIndex) SonaTheme.colors.UserBubble else SonaTheme.colors.AiBubble)
                        .clickable { state.onSelectRole(idx) }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(role.name, fontWeight = FontWeight.Bold, color = SonaTheme.colors.AiText)
                        Text(role.short, color = SonaTheme.colors.BackgroundText, fontSize = 12.sp)
                    }
                    ActionButton(onClick = { state.onEditRole(idx) }) { Text("✏") }
                    Spacer(Modifier.width(8.dp))
                    ActionButton(
                        onClick = { deleteIndex = idx },
                        enabled = role.name !in DefaultRoles.NAMES
                    ) { Text("\uD83D\uDDD1") }
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(color = SonaTheme.colors.UserBubble)
                    .clickable { state.onAddRole() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(Strings.addRole, color = Color.White, fontSize = 12.sp)
            }
        }
        deleteIndex?.let { idx ->
            DeleteConfirmationDialog(
                text = Strings.deleteRoleQuestion,
                onConfirm = {
                    state.onDeleteRole(idx)
                    deleteIndex = null
                },
                onCancel = { deleteIndex = null }
            )
        }
    }
}

@Composable
fun RolesPanel(state: State.EditRoleState) {
    val nameState = rememberTextFieldState(state.role.name)
    val shortState = rememberTextFieldState(state.role.short)
    val textState = rememberTextFieldState(state.role.text)

    LaunchedEffect(state.role) {
        nameState.setTextAndPlaceCursorAtEnd(state.role.name)
        shortState.setTextAndPlaceCursorAtEnd(state.role.short)
        textState.setTextAndPlaceCursorAtEnd(state.role.text)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(SonaTheme.colors.Background)
            .padding(16.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Text(Strings.name)
            Spacer(Modifier.height(6.dp))
            TextField(nameState, Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Text(Strings.shortDescription)
            Spacer(Modifier.height(6.dp))
            TextField(shortState, Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Text(Strings.context)
            Spacer(Modifier.height(6.dp))
            TextArea(
                textState,
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SonaTheme.colors.InputBackground)
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                ActionButton(onClick = state.onCancel, modifier = Modifier.weight(1f)) { Text(Strings.cancel) }
                Spacer(Modifier.width(8.dp))
                ActionButton(
                    onClick = {
                        state.onSave(
                            Role(
                                nameState.text.toString(),
                                shortState.text.toString(),
                                textState.text.toString()
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(Strings.save) }
            }
        }
    }
}

