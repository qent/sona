package io.qent.sona.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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
 */
@Composable
fun DropdownSelector(
    items: List<String>,
    selectedIndex: Int,
    expandUpwards: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    buttonModifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var buttonPosition by remember { mutableStateOf(Offset.Zero) }
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
        ActionButton(
            onClick = { expanded = !expanded },
            modifier = buttonModifier.onGloballyPositioned {
                buttonPosition = it.positionInRoot()
                buttonSize = it.size
            }
        ) {
            Text(items.getOrElse(selectedIndex, { "" }))
        }

        if (expanded && filtered.isNotEmpty()) {
            Popup(
                offset = IntOffset(
                    x = buttonPosition.x.toInt(),
                    y = if (expandUpwards) (buttonPosition.y - buttonSize.height * filtered.size).toInt() else (buttonPosition.y + buttonSize.height).toInt()
                ),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true)
            ) {
                Column(
                    modifier = Modifier
                        .width(with(LocalDensity.current) { buttonSize.width.toDp() })
                        .clip(RoundedCornerShape(4.dp))
                        .background(SonaTheme.colors.InputBackground)
                        .zIndex(1f)
                ) {
                    if (!expandUpwards) {
                        Text(
                            items[selectedIndex],
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                    filtered.forEach { (idx, name) ->
                        ActionButton(
                            onClick = {
                                expanded = false
                                onSelect(idx)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(name) }
                    }
                    if (expandUpwards) {
                        Text(
                            items[selectedIndex],
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
