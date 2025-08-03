package io.qent.sona.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Dp.dp2int(): Int {
    val density = LocalDensity.current
    val verticalPadding = 6.dp // Your vertical padding
    return with(density) { verticalPadding.toPx() }.toInt()
}