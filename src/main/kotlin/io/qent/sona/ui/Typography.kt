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

val MarkdownTextColor = Color(0xFFA9B7C6)
val MarkdownCodeForegroundColor = Color(0xFFBBB529)
val MarkdownInlineCodeForegroundColor = Color(0xFFFFC66D)
val MarkdownLinkColor = Color(0xFF589DF6)
val MarkdownCodeBackgroundColor = Color(0xFF2B2B2B)
val MarkdownInlineCodeBackgroundColor = Color(0xFF3C3F41)
val MarkdownQuoteColor = Color(0xFF629755)
val MarkdownDividerColor = Color(0xFF3C3F41)
val MarkdownTableBackgroundColor = Color(0xFF2B2B2B)
val MarkdownTableTextColor = Color(0xFFA9B7C6)
val MarkdownLinkHoveredColor = Color(0xFF40C4FF)
val MarkdownLinkFocusedColor = Color(0xFF9876AA)
val MarkdownLinkPressedColor = Color(0xFF40C4FF)

object Typography {
    val Dark = DefaultMarkdownTypography(
        h1 = TextStyle(
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MarkdownTextColor
        ),
        h2 = TextStyle(
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MarkdownTextColor
        ),
        h3 = TextStyle(
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MarkdownTextColor
        ),
        h4 = TextStyle(
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MarkdownTextColor
        ),
        h5 = TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MarkdownTextColor
        ),
        h6 = TextStyle(
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MarkdownTextColor
        ),
        text = TextStyle(
            fontSize = 14.sp,
            color = MarkdownTextColor
        ),
        code = TextStyle(
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            color = MarkdownCodeForegroundColor,
            background = MarkdownCodeBackgroundColor
        ),
        inlineCode = TextStyle(
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            color = MarkdownInlineCodeForegroundColor,
            background = MarkdownInlineCodeBackgroundColor
        ),
        quote = TextStyle(
            fontSize = 14.sp,
            color = MarkdownQuoteColor,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
        ),
        paragraph = TextStyle(
            fontSize = 14.sp,
            color = MarkdownTextColor
        ),
        ordered = TextStyle(
            fontSize = 14.sp,
            color = MarkdownTextColor
        ),
        bullet = TextStyle(
            fontSize = 14.sp,
            color = MarkdownTextColor
        ),
        list = TextStyle(
            fontSize = 14.sp,
            color = MarkdownTextColor
        ),
        link = TextStyle(
            fontSize = 14.sp,
            color = MarkdownLinkColor,
            textDecoration = TextDecoration.Underline
        ),
        textLink = TextLinkStyles(
            style = SpanStyle(
                color = MarkdownLinkColor,
                textDecoration = TextDecoration.Underline
            ),
            hoveredStyle = SpanStyle(
                color = MarkdownLinkHoveredColor
            ),
            focusedStyle = SpanStyle(
                color = MarkdownLinkFocusedColor
            ),
            pressedStyle = SpanStyle(
                color = MarkdownLinkPressedColor
            )
        ),
        table = TextStyle(
            fontSize = 14.sp,
            color = MarkdownTableTextColor
        ),
    )
}

object Colors {
    val Dark = DefaultMarkdownColors(
        text = MarkdownTextColor,
        codeText = MarkdownCodeForegroundColor,
        inlineCodeText = MarkdownInlineCodeForegroundColor,
        linkText = MarkdownLinkColor,
        codeBackground = MarkdownCodeBackgroundColor,
        inlineCodeBackground = MarkdownInlineCodeBackgroundColor,
        dividerColor = MarkdownDividerColor,
        tableText = MarkdownTableTextColor,
        tableBackground = MarkdownTableBackgroundColor,
    )
}
