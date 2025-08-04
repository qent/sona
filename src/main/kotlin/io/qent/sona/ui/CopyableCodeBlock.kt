package io.qent.sona.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.compose.components.MarkdownComponentModel
import com.mikepenz.markdown.compose.elements.MarkdownCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownCodeFence
import org.jetbrains.jewel.ui.component.IconButton

/**
 * Renders a markdown code block with a copy button overlay.
 */
@Composable
fun CopyableCodeBlock(model: MarkdownComponentModel, fence: Boolean) {
    val clipboard = LocalClipboardManager.current
    Box {
        if (fence) {
            MarkdownCodeFence(model.content, model.node, style = model.typography.code)
        } else {
            MarkdownCodeBlock(model.content, model.node, style = model.typography.code)
        }
        IconButton(onClick = { clipboard.setText(AnnotatedString(model.content)) }, modifier = Modifier.align(Alignment.TopEnd)) {
            Image(
                painter = loadIcon("/icons/copy.svg"),
                contentDescription = "Copy code",
                colorFilter = ColorFilter.tint(SonaTheme.colors.AiText),
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
