package io.qent.sona.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.minus
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import org.jetbrains.jewel.ui.component.ActionButton
import org.jetbrains.jewel.ui.component.Text

/**
 * Reusable dropdown selector that can expand either upward or downward.
 *
 * @param items list of display names.
 * @param selectedIndex index of currently selected item.
 * @param expandUpwards if true the list expands upward, otherwise downward.
 * @param onSelect callback invoked with the index of the chosen item.
 * @param modifier modifier applied to the container box.
 * @param buttonModifier modifier applied to the toggle button.
 * @param textAlign horizontal alignment for the text inside buttons.
 */
@Composable
fun DropdownSelector(
    items: List<String>,
    selectedIndex: Int,
    expandUpwards: Boolean,
    onSelect: (Int) -> Unit,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    buttonModifier: Modifier = Modifier,
    textAlign: Alignment = Alignment.CenterStart
) {
    var expanded by remember { mutableStateOf(false) }
    var buttonSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    val currentItems by rememberUpdatedState(items)
    val filtered = remember(currentItems, selectedIndex) {
        currentItems.mapIndexedNotNull { index, name ->
            if (index != selectedIndex) index to name else null
        }
    }

    Box(
        modifier = modifier.onGloballyPositioned { }
    ) {
        if (expanded && filtered.isNotEmpty()) {
            Popup(
                offset = IntOffset(
                    x = -4,
                    y = if (expandUpwards) -(buttonSize.height * items.size) - 4 else 0
                ),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true)
            ) {
                Column(
                    modifier = Modifier
                        .width(with(LocalDensity.current) { buttonSize.width.toDp() })
                        .background(backgroundColor, RoundedCornerShape(12.dp))
                        .padding(2.dp, 2.dp)
                        .zIndex(1f)
                ) {
                    if (!expandUpwards) {
                        ActionButton(
                            onClick = { expanded = !expanded },
                            modifier = buttonModifier.fillMaxWidth()
                        ) {
                            ButtonText(
                                items.getOrElse(selectedIndex, { "" }),
                                textAlign
                            )
                        }
                    }
                    filtered.forEach { (idx, name) ->
                        ActionButton(
                            onClick = {
                                expanded = false
                                onSelect(idx)
                            },
                            modifier = buttonModifier.fillMaxWidth()
                        ) {
                            ButtonText(
                                name,
                                textAlign
                            )
                        }
                    }
                    if (expandUpwards) {
                        ActionButton(
                            onClick = { expanded = !expanded },
                            modifier = buttonModifier.fillMaxWidth()
                        ) {
                            ButtonText(
                                items.getOrElse(selectedIndex, { "" }),
                                textAlign
                            )
                        }
                    }
                }
            }
        } else {
            ActionButton(
                onClick = { expanded = !expanded },
                modifier = buttonModifier.width(110.dp).onGloballyPositioned {
                    buttonSize = it.size
                }.background(backgroundColor, RoundedCornerShape(12.dp))
            ) {
                ButtonText(
                    items.getOrElse(selectedIndex, { "" }),
                    textAlign
                )
            }
        }
    }
}

@Composable
private fun ButtonText(text: String, textAlign: Alignment) {
    Box(
        Modifier.fillMaxWidth().padding(6.dp, 6.dp),
        contentAlignment = textAlign
    ) {
        Text(text)
    }
}
