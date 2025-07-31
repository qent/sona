package io.qent.sona.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.qent.sona.core.State.RolesState
import org.jetbrains.jewel.ui.component.ActionButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextArea

@Composable
fun RolesPanel(state: RolesState) {
    val textState = rememberTextFieldState(state.text)
    Column(Modifier.fillMaxSize().padding(8.dp)) {
        TextArea(
            textState,
            Modifier.weight(1f).fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        ActionButton(onClick = { state.onSave(textState.text.toString()) }, modifier = Modifier.fillMaxWidth()) {
            Text("Save")
        }
    }
}