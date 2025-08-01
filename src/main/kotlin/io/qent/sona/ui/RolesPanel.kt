package io.qent.sona.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.qent.sona.core.State.RolesState
import org.jetbrains.jewel.ui.component.ActionButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextArea
import org.jetbrains.jewel.ui.component.TextField

@Composable
fun RolesPanel(state: RolesState) {
    val textState = rememberTextFieldState(state.text)
    var menuExpanded by remember { mutableStateOf(false) }
    val nameState = rememberTextFieldState()
    Column(Modifier.fillMaxSize().padding(8.dp)) {
        Row(Modifier.fillMaxWidth()) {
            if (state.creating) {
                TextField(nameState, Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                ActionButton(onClick = {
                    state.onAddRole(nameState.text.toString(), textState.text.toString())
                    nameState.clearText()
                }) { Text("Save") }
            } else {
                Column(Modifier.weight(1f)) {
                    ActionButton(onClick = { menuExpanded = !menuExpanded }, modifier = Modifier.fillMaxWidth()) {
                        Text(state.roles[state.currentIndex])
                    }
                    if (menuExpanded) {
                        state.roles.forEachIndexed { idx, name ->
                            ActionButton(
                                onClick = {
                                    menuExpanded = false
                                    state.onSelectRole(idx)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(name) }
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                ActionButton(onClick = { nameState.clearText(); textState.clearText(); state.onStartCreateRole() }) { Text("+") }
                Spacer(Modifier.width(8.dp))
                ActionButton(onClick = { state.onDeleteRole() }, enabled = state.roles.size > 1) { Text("\uD83D\uDDD1") }
            }
        }
        Spacer(Modifier.height(8.dp))
        TextArea(
            textState,
            Modifier.weight(1f).fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        if (!state.creating) {
            ActionButton(onClick = { state.onSave(textState.text.toString()) }, modifier = Modifier.fillMaxWidth()) {
                Text("Save")
            }
        }
    }
}