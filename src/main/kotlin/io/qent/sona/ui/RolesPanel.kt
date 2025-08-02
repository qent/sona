package io.qent.sona.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.qent.sona.core.State.RolesState
import org.jetbrains.jewel.ui.component.ActionButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextArea

@Composable
fun RolesPanel(state: RolesState) {
    val textState = rememberTextFieldState(state.text)
    val nameState = rememberTextFieldState()

    LaunchedEffect(state.currentIndex) {
        if (textState.text != state.text) {
            textState.setTextAndPlaceCursorAtEnd(state.text)
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(SonaTheme.colors.Background)
    ) {
        // Base screen content
        Column(
            Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Row(Modifier.fillMaxWidth()) {
                DropdownSelector(
                    items = state.roles,
                    selectedIndex = state.currentIndex,
                    expandUpwards = false,
                    onSelect = { state.onSelectRole(it) },
                    modifier = Modifier.weight(1f),
                    buttonModifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.width(8.dp))
                ActionButton(
                    onClick = {
                        nameState.clearText()
                        textState.clearText()
                        state.onStartCreateRole()
                    }
                ) { Text("+") }

                Spacer(Modifier.width(8.dp))
                ActionButton(
                    onClick = { state.onDeleteRole() },
                    enabled = state.roles.size > 1
                ) { Text("\uD83D\uDDD1") }
            }

            Spacer(Modifier.height(8.dp))

            TextArea(
                textState,
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SonaTheme.colors.InputBackground)
            )

            Spacer(Modifier.height(8.dp))

            if (!state.creating) {
                ActionButton(
                    onClick = { state.onSave(textState.text.toString()) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save")
                }
            }
        }
    }
}
