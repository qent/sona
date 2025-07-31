package io.qent.sona.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.model.DefaultMarkdownColors
import com.mikepenz.markdown.model.DefaultMarkdownTypography

val MarkdownDarkTextColor = Color(0xFFA9B7C6)
val MarkdownDarkCodeForegroundColor = Color(0xFFBBB529)
val MarkdownDarkInlineCodeForegroundColor = Color(0xFFFFC66D)
val MarkdownDarkLinkColor = Color(0xFF589DF6)
val MarkdownDarkCodeBackgroundColor = Color(0xFF2B2B2B)
val MarkdownDarkInlineCodeBackgroundColor = Color(0xFF3C3F41)
val MarkdownDarkQuoteColor = Color(0xFF629755)
val MarkdownDarkDividerColor = Color(0xFF3C3F41)
val MarkdownDarkTableBackgroundColor = Color(0xFF2B2B2B)
val MarkdownDarkTableTextColor = Color(0xFFA9B7C6)
val MarkdownDarkLinkHoveredColor = Color(0xFF40C4FF)
val MarkdownDarkLinkFocusedColor = Color(0xFF9876AA)
val MarkdownDarkLinkPressedColor = Color(0xFF40C4FF)

val MarkdownLightTextColor = Color(0xFF000000)
val MarkdownLightCodeForegroundColor = Color(0xFF6A8759)
val MarkdownLightInlineCodeForegroundColor = Color(0xFF6A8759)
val MarkdownLightLinkColor = Color(0xFF0066CC)
val MarkdownLightCodeBackgroundColor = Color(0xFFE6E6E6)
val MarkdownLightInlineCodeBackgroundColor = Color(0xFFDADADA)
val MarkdownLightQuoteColor = Color(0xFF6A8759)
val MarkdownLightDividerColor = Color(0xFFDADADA)
val MarkdownLightTableBackgroundColor = Color(0xFFE6E6E6)
val MarkdownLightTableTextColor = Color(0xFF000000)
val MarkdownLightLinkHoveredColor = Color(0xFF005BB5)
val MarkdownLightLinkFocusedColor = Color(0xFF663399)
val MarkdownLightLinkPressedColor = Color(0xFF005BB5)

object Typography {
    val Dark = DefaultMarkdownTypography(
        h1 = TextStyle(
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MarkdownDarkTextColor
        ),
        h2 = TextStyle(
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MarkdownDarkTextColor
        ),
        h3 = TextStyle(
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MarkdownDarkTextColor
        ),
        h4 = TextStyle(
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MarkdownDarkTextColor
        ),
        h5 = TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MarkdownDarkTextColor
        ),
        h6 = TextStyle(
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MarkdownDarkTextColor
        ),
        text = TextStyle(
            fontSize = 14.sp,
            color = MarkdownDarkTextColor
        ),
        code = TextStyle(
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            color = MarkdownDarkCodeForegroundColor,
            background = MarkdownDarkCodeBackgroundColor
        ),
        inlineCode = TextStyle(
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            color = MarkdownDarkInlineCodeForegroundColor,
            background = MarkdownDarkInlineCodeBackgroundColor
        ),
        quote = TextStyle(
            fontSize = 14.sp,
            color = MarkdownDarkQuoteColor,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
        ),
        paragraph = TextStyle(
            fontSize = 14.sp,
            color = MarkdownDarkTextColor
        ),
        ordered = TextStyle(
            fontSize = 14.sp,
            color = MarkdownDarkTextColor
        ),
        bullet = TextStyle(
            fontSize = 14.sp,
            color = MarkdownDarkTextColor
        ),
        list = TextStyle(
            fontSize = 14.sp,
            color = MarkdownDarkTextColor
        ),
        link = TextStyle(
            fontSize = 14.sp,
            color = MarkdownDarkLinkColor,
            textDecoration = TextDecoration.Underline
        ),
        textLink = TextLinkStyles(
            style = SpanStyle(
                color = MarkdownDarkLinkColor,
                textDecoration = TextDecoration.Underline
            ),
            hoveredStyle = SpanStyle(
                color = MarkdownDarkLinkHoveredColor
            ),
            focusedStyle = SpanStyle(
                color = MarkdownDarkLinkFocusedColor
            ),
            pressedStyle = SpanStyle(
                color = MarkdownDarkLinkPressedColor
            )
        ),
        table = TextStyle(
            fontSize = 14.sp,
            color = MarkdownDarkTableTextColor
        ),
    )

    val Light = DefaultMarkdownTypography(
        h1 = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MarkdownLightTextColor),
        h2 = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MarkdownLightTextColor),
        h3 = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MarkdownLightTextColor),
        h4 = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MarkdownLightTextColor),
        h5 = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MarkdownLightTextColor),
        h6 = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MarkdownLightTextColor),
        text = TextStyle(fontSize = 14.sp, color = MarkdownLightTextColor),
        code = TextStyle(
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            color = MarkdownLightCodeForegroundColor,
            background = MarkdownLightCodeBackgroundColor
        ),
        inlineCode = TextStyle(
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            color = MarkdownLightInlineCodeForegroundColor,
            background = MarkdownLightInlineCodeBackgroundColor
        ),
        quote = TextStyle(
            fontSize = 14.sp,
            color = MarkdownLightQuoteColor,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
        ),
        paragraph = TextStyle(fontSize = 14.sp, color = MarkdownLightTextColor),
        ordered = TextStyle(fontSize = 14.sp, color = MarkdownLightTextColor),
        bullet = TextStyle(fontSize = 14.sp, color = MarkdownLightTextColor),
        list = TextStyle(fontSize = 14.sp, color = MarkdownLightTextColor),
        link = TextStyle(fontSize = 14.sp, color = MarkdownLightLinkColor, textDecoration = TextDecoration.Underline),
        textLink = TextLinkStyles(
            style = SpanStyle(color = MarkdownLightLinkColor, textDecoration = TextDecoration.Underline),
            hoveredStyle = SpanStyle(color = MarkdownLightLinkHoveredColor),
            focusedStyle = SpanStyle(color = MarkdownLightLinkFocusedColor),
            pressedStyle = SpanStyle(color = MarkdownLightLinkPressedColor)
        ),
        table = TextStyle(fontSize = 14.sp, color = MarkdownLightTableTextColor),
    )
}

object MarkdownColors {
    val Dark = DefaultMarkdownColors(
        text = MarkdownDarkTextColor,
        codeText = MarkdownDarkCodeForegroundColor,
        inlineCodeText = MarkdownDarkInlineCodeForegroundColor,
        linkText = MarkdownDarkLinkColor,
        codeBackground = MarkdownDarkCodeBackgroundColor,
        inlineCodeBackground = MarkdownDarkInlineCodeBackgroundColor,
        dividerColor = MarkdownDarkDividerColor,
        tableText = MarkdownDarkTableTextColor,
        tableBackground = MarkdownDarkTableBackgroundColor,
    )

    val Light = DefaultMarkdownColors(
        text = MarkdownLightTextColor,
        codeText = MarkdownLightCodeForegroundColor,
        inlineCodeText = MarkdownLightInlineCodeForegroundColor,
        linkText = MarkdownLightLinkColor,
        codeBackground = MarkdownLightCodeBackgroundColor,
        inlineCodeBackground = MarkdownLightInlineCodeBackgroundColor,
        dividerColor = MarkdownLightDividerColor,
        tableText = MarkdownLightTableTextColor,
        tableBackground = MarkdownLightTableBackgroundColor,
    )
}
