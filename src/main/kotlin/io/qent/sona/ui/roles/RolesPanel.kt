package io.qent.sona.ui.roles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.qent.sona.Strings
import io.qent.sona.core.roles.DefaultRoles
import io.qent.sona.core.state.State.RolesState
import io.qent.sona.ui.DropdownSelector
import io.qent.sona.ui.SonaTheme
import org.jetbrains.jewel.ui.component.ActionButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextArea
import org.jetbrains.jewel.ui.component.TextField

@Composable
fun RolesPanel(state: RolesState) {
    val textState = rememberTextFieldState(state.text)
    val shortState = rememberTextFieldState(state.short)
    val nameState = rememberTextFieldState()

    LaunchedEffect(state.currentIndex) {
        if (textState.text != state.text) {
            textState.setTextAndPlaceCursorAtEnd(state.text)
        }
        if (shortState.text != state.short) {
            shortState.setTextAndPlaceCursorAtEnd(state.short)
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(SonaTheme.colors.Background)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Text(
                Strings.agentRoles,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
                style = SonaTheme.markdownTypography.h5
            )
            Row(Modifier.fillMaxWidth()) {
                if (state.creating) {
                    TextField(nameState, Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    ActionButton(onClick = {
                        state.onAddRole(
                            nameState.text.toString(),
                            shortState.text.toString(),
                            textState.text.toString()
                        )
                        nameState.clearText()
                        shortState.clearText()
                      }) { Text(Strings.save) }
                } else {
                    DropdownSelector(
                        items = state.roles,
                        selectedIndex = state.currentIndex,
                        expandUpwards = false,
                        onSelect = { state.onSelectRole(it) },
                        modifier = Modifier.weight(1f),
                        backgroundColor = SonaTheme.colors.Background,
                        buttonModifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.width(8.dp))
                    ActionButton(
                        onClick = {
                            nameState.clearText()
                            textState.clearText()
                            shortState.clearText()
                            state.onStartCreateRole()
                        }
                      ) { Text("+") }

                    Spacer(Modifier.width(8.dp))
                    ActionButton(
                        onClick = { state.onDeleteRole() },
                        enabled = state.roles.size > DefaultRoles.NAMES.size &&
                            state.roles[state.currentIndex] !in DefaultRoles.NAMES
                      ) { Text("\uD83D\uDDD1") }
                }
            }

            Spacer(Modifier.height(8.dp))

            TextArea(
                shortState,
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SonaTheme.colors.InputBackground)
            )

            Spacer(Modifier.height(8.dp))

            TextArea(
                textState,
                Modifier
                    .weight(3f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SonaTheme.colors.InputBackground)
            )

            Spacer(Modifier.height(8.dp))

            if (!state.creating) {
                ActionButton(
                    onClick = { state.onSave(shortState.text.toString(), textState.text.toString()) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                  Text(Strings.save)
                }
            }
        }
    }
}
