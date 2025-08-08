package io.qent.sona.ui.chat

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.qent.sona.Strings
import io.qent.sona.core.roles.DefaultRoles
import io.qent.sona.core.state.State.ChatState
import org.jetbrains.jewel.ui.component.ActionButton
import org.jetbrains.jewel.ui.component.Text
import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import io.qent.sona.ui.DropdownSelector
import io.qent.sona.ui.SonaTheme

@Composable
fun ChatInput(state: ChatState) {
    val text = remember { mutableStateOf(TextFieldValue("")) }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val placeholder = if (state.messages.isNotEmpty() || state.roles.isEmpty()) {
        Strings.typeMessage
    } else {
        when (state.roles[state.activeRole]) {
            DefaultRoles.ARCHITECT.displayName -> Strings.architectPlaceholder
            DefaultRoles.CODE.displayName -> Strings.codePlaceholder
            else -> Strings.defaultPlaceholder
        }
    }

    Box(
        Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .background(SonaTheme.colors.Background, RoundedCornerShape(12.dp))
                    .border(
                        1.dp,
                        if (isFocused) SonaTheme.colors.BorderFocused else SonaTheme.colors.BorderDefault,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (text.value.text.isEmpty()) {
                    Text(
                        placeholder,
                        color = SonaTheme.colors.Placeholder,
                        fontSize = 14.sp
                    )
                }
                BasicTextField(
                    value = text.value,
                    onValueChange = { text.value = it },
                    enabled = !state.isSending,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .heightIn(min = 100.dp, max = 300.dp)
                        .padding(bottom = 30.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged { isFocused = it.isFocused }
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                                if (event.isShiftPressed) {
                                    val oldValue = text.value
                                    val selection = oldValue.selection
                                    val newText = StringBuilder(oldValue.text).insert(selection.start, "\n").toString()
                                    val newCursor = selection.start + 1
                                    text.value = oldValue.copy(
                                        text = newText,
                                        selection = TextRange(newCursor)
                                    )
                                    true
                                } else {
                                    if (text.value.text.isNotBlank()) {
                                        state.onSendMessage(text.value.text)
                                        text.value = TextFieldValue("")
                                    }
                                    true
                                }
                            } else {
                                false
                            }
                        },
                    cursorBrush = SolidColor(Color.White),
                    textStyle = TextStyle(
                        color = SonaTheme.colors.UserText,
                        fontSize = 14.sp
                    ),
                    singleLine = false,
                )
            }
        }

        DropdownSelector(
            items = state.roles,
            selectedIndex = state.activeRole,
            expandUpwards = true,
            onSelect = { state.onSelectRole(it) },
            backgroundColor = Color.White.copy(alpha = 0.2f),
            modifier = Modifier.align(Alignment.BottomStart).padding(6.dp).alpha(0.6f)
        )

        DropdownSelector(
            items = state.presets.presets.map { it.name },
            selectedIndex = state.presets.active,
            expandUpwards = true,
            onSelect = { state.onSelectPreset(it) },
            backgroundColor = Color.White.copy(alpha = 0.2f),
            modifier = Modifier.align(Alignment.BottomStart).padding(6.dp).offset(x = 115.dp).alpha(0.6f)
        )


        Row(
            modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionButton(
                onClick = { state.onToggleAutoApprove() },
                modifier = Modifier
                    .height(30.dp)
                    .width(30.dp)
                    .clip(CircleShape)
                    .alpha(if (state.autoApproveTools) 0.6f else 0.3f)
            ) {
                Text("🤘")
            }
            Spacer(Modifier.width(4.dp))
            if (state.isSending) {
                val transition = rememberInfiniteTransition(label = "stopPulse")
                val scale by transition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = keyframes {
                            durationMillis = 1500
                            0.95f at 200
                            1.1f at 400
                            0.8f at 700
                            1.10f at 1000
                            0.85f at 1300
                            1.15f at 1600
                            0.9f at 2000
                        }
                    ),
                    label = "scale",
                )

                Box(
                    modifier = Modifier.size(30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    )

                    ActionButton(
                        onClick = { state.onStop() },
                        modifier = Modifier
                            .matchParentSize()
                            .clip(CircleShape),
                    ) {
                        Text("■")
                    }
                }
            } else {
                ActionButton(
                    onClick = {
                        if (text.value.text.isNotBlank()) {
                            state.onSendMessage(text.value.text)
                            text.value = TextFieldValue("")
                        }
                    },
                    enabled = text.value.text.isNotBlank(),
                    modifier = Modifier
                        .height(30.dp)
                        .width(30.dp)
                        .clip(CircleShape)
                        .alpha(alpha = if (text.value.text.isBlank()) 0.4f else 1f),
                ) {
                    Text("➤")
                }
            }
        }
    }
}
