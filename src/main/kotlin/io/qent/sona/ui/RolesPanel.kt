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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import androidx.compose.ui.zIndex
import io.qent.sona.core.State.RolesState
import org.jetbrains.jewel.ui.component.ActionButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextArea

@Composable
fun RolesPanel(state: RolesState) {
    val textState = rememberTextFieldState(state.text)
    var menuExpanded by remember { mutableStateOf(false) }
    val nameState = rememberTextFieldState()

    var buttonPosition by remember { mutableStateOf(Offset.Zero) }
    var buttonSize by remember { mutableStateOf(IntSize.Zero) }

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

                // Role selector button wrapped in Box so we can capture its position
                Box(Modifier.weight(1f)) {
                    ActionButton(
                        onClick = { menuExpanded = !menuExpanded },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coords ->
                                buttonPosition = coords.positionInRoot()
                                buttonSize = coords.size
                            }
                    ) {
                        Text(state.roles[state.currentIndex])
                    }
                }

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

        // Overlay dropdown menu – sits on top of everything else without affecting layout
        if (menuExpanded) {
            Column(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = buttonPosition.x.toInt(),
                            y = buttonPosition.y.toInt() + buttonSize.height
                        )
                    }
                    .width(with(LocalDensity.current) { buttonSize.width.toDp() })
                    .clip(RoundedCornerShape(4.dp))
                    .background(SonaTheme.colors.InputBackground)
                    .zIndex(1f)
            ) {
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
    }
}