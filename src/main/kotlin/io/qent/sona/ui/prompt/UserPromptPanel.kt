package io.qent.sona.ui.prompt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.qent.sona.Strings
import io.qent.sona.core.state.State
import io.qent.sona.ui.common.SonaTheme
import org.jetbrains.jewel.ui.component.ActionButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextArea

@Composable
fun UserPromptPanel(state: State.UserPromptState) {
    val textState = rememberTextFieldState(state.prompt)
    LaunchedEffect(state.prompt) {
        textState.setTextAndPlaceCursorAtEnd(state.prompt)
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(SonaTheme.colors.Background)
            .padding(16.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Text(Strings.userSystemPrompt)
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
            ActionButton(onClick = { state.onSave(textState.text.toString()) }, modifier = Modifier.fillMaxWidth()) {
                Text(Strings.save)
            }
        }
    }
}
