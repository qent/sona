package io.qent.sona.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.model.DefaultMarkdownColors
import com.mikepenz.markdown.model.DefaultMarkdownTypography

@Composable
fun MarkdownText(text: String) {
    Markdown(
        text,
        colors = DefaultMarkdownColors(
            text = Color(0xFFA9B7C6),            // @Foreground
            codeText = Color(0xFFBBB529),         // @CodeForeground (устарело)
            inlineCodeText = Color(0xFFFFC66D),   // чуть светлее для inline-кода (устарело)
            linkText = Color(0xFF589DF6),         // @LinkForeground (устарело)
            codeBackground = Color(0xFF2B2B2B),   // @CodeBlockBackground
            inlineCodeBackground = Color(0xFF3C3F41), // чуть светлее для inline
            dividerColor = Color(0xFF3C3F41),     // @SeparatorColor
            tableText = Color(0xFFA9B7C6),        // как основной текст (устарело)
            tableBackground = Color(0xFF2B2B2B),  // @EditorBackground
        ),
        typography = DefaultMarkdownTypography(
            h1 = TextStyle(
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFA9B7C6) // @Foreground
            ),
            h2 = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFA9B7C6)
            ),
            h3 = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFA9B7C6)
            ),
            h4 = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFA9B7C6)
            ),
            h5 = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFA9B7C6)
            ),
            h6 = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFA9B7C6)
            ),
            text = TextStyle(
                fontSize = 14.sp,
                color = Color(0xFFA9B7C6)
            ),
            code = TextStyle(
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFBBB529),
                background = Color(0xFF2B2B2B)
            ),
            inlineCode = TextStyle(
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFFFC66D),
                background = Color(0xFF3C3F41)
            ),
            quote = TextStyle(
                fontSize = 14.sp,
                color = Color(0xFF629755), // зеленоватый Darcula для quote
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            ),
            paragraph = TextStyle(
                fontSize = 14.sp,
                color = Color(0xFFA9B7C6)
            ),
            ordered = TextStyle(
                fontSize = 14.sp,
                color = Color(0xFFA9B7C6)
            ),
            bullet = TextStyle(
                fontSize = 14.sp,
                color = Color(0xFFA9B7C6)
            ),
            list = TextStyle(
                fontSize = 14.sp,
                color = Color(0xFFA9B7C6)
            ),
            link = TextStyle(
                fontSize = 14.sp,
                color = Color(0xFF589DF6), // синий ссылочный
                textDecoration = TextDecoration.Underline
            ),
            textLink = TextLinkStyles(
                style = SpanStyle(
                    color = Color(0xFF589DF6), // основной стиль ссылки (Darcula blue)
                    textDecoration = TextDecoration.Underline
                ),
                hoveredStyle = SpanStyle(
                    color = Color(0xFF40C4FF) // более яркий голубой при ховере
                ),
                focusedStyle = SpanStyle(
                    color = Color(0xFF9876AA) // фиолетовый для "фокуса" (или для "visited" использовать, если нужен)
                ),
                pressedStyle = SpanStyle(
                    color = Color(0xFF40C4FF) // например, тот же как hovered, можешь поменять
                )
            ),
            table = TextStyle(
                fontSize = 14.sp,
                color = Color(0xFFA9B7C6)
            ),
        )
    )
}