package io.qent.sona.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.qent.sona.Strings
import org.jetbrains.jewel.ui.component.ActionButton
import org.jetbrains.jewel.ui.component.Text

@Composable
fun DeleteConfirmationDialog(text: String, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Dialog(onDismissRequest = onCancel) {
        Box(
            Modifier
                .background(SonaTheme.colors.AiBubble, RoundedCornerShape(8.dp))
                .border(1.dp, SonaTheme.colors.BorderDefault, RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(text)
                Spacer(Modifier.height(12.dp))
                Row {
                    ActionButton(onClick = onConfirm, modifier = Modifier.weight(1f)) { Text(Strings.ok) }
                    Spacer(Modifier.width(8.dp))
                    ActionButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text(Strings.cancel) }
                }
            }
        }
    }
}

