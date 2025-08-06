package io.qent.sona.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class SonaColors(
    val Background: Color,
    val BackgroundText: Color,
    val UserBubble: Color,
    val AiBubble: Color,
    val UserText: Color,
    val AiText: Color,
    val InputBackground: Color,
    val Placeholder: Color,
    val BubbleShadow: Color,
    val BorderFocused: Color,
    val BorderDefault: Color,
    val AvatarBackground: Color,
)

private val DarkPalette = SonaColors(
    Background = Color(0xFF20232A),
    BackgroundText = Color(0xFF7A818A),
    UserBubble = Color(0xFF3366FF),
    AiBubble = Color(0xFF292D36),
    UserText = Color.White,
    AiText = Color(0xFFD3D8DF),
    InputBackground = Color(0xFF22262A),
    Placeholder = Color(0xFF7A818A),
    BubbleShadow = Color(0x22000000),
    BorderFocused = Color(0xFF3B72FF),
    BorderDefault = Color(0xFF373B42),
    AvatarBackground = Color(0xFF8E98A9),
)

private val LightPalette = SonaColors(
    Background = Color(0xFFFFFFFF),
    BackgroundText = Color(0xFF6E6E6E),
    UserBubble = Color(0xFF3366FF),
    AiBubble = Color(0xFFF0F0F0),
    UserText = Color.Black,
    AiText = Color(0xFF333333),
    InputBackground = Color(0xFFE8E8E8),
    Placeholder = Color(0xFF6E6E6E),
    BubbleShadow = Color(0x22000000),
    BorderFocused = Color(0xFF3B72FF),
    BorderDefault = Color(0xFFC4C4C4),
    AvatarBackground = Color(0xFFC8C8C8),
)

private val LocalColors = staticCompositionLocalOf { DarkPalette }
private val LocalMarkdownColors = staticCompositionLocalOf { MarkdownColors.Dark }
private val LocalTypography = staticCompositionLocalOf { Typography.Dark }

@Composable
fun SonaTheme(dark: Boolean, content: @Composable () -> Unit) {
    val colors = if (dark) DarkPalette else LightPalette
    val mdColors = if (dark) MarkdownColors.Dark else MarkdownColors.Light
    val mdTypography = if (dark) Typography.Dark else Typography.Light
    CompositionLocalProvider(
        LocalColors provides colors,
        LocalMarkdownColors provides mdColors,
        LocalTypography provides mdTypography,
        content = content
    )
}

object SonaTheme {
    val colors: SonaColors
        @Composable get() = LocalColors.current
    val markdownColors
        @Composable get() = LocalMarkdownColors.current
    val markdownTypography
        @Composable get() = LocalTypography.current
}
