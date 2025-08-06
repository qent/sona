package io.qent.sona.ui.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.intellij.lang.Language
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorTextField
import com.mikepenz.markdown.compose.components.MarkdownComponentModel
import com.mikepenz.markdown.compose.elements.MarkdownCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownCodeFence
import io.qent.sona.ui.SonaTheme
import org.jetbrains.jewel.ui.component.IconButton

/**
 * Renders a markdown code block with a copy button overlay.
 */
@Composable
fun CopyableCodeBlock(project: Project, model: MarkdownComponentModel, fence: Boolean) {
    val clipboard = LocalClipboardManager.current
    val extractedCode = remember { mutableStateOf("") }

    Box {
        if (fence) {
            MarkdownCodeFence(model.content, model.node, style = model.typography.code) { code: String, lang: String?, _ ->
                extractedCode.value = code
                CodeEditor(project, code, lang)
            }
        } else {
            MarkdownCodeBlock(model.content, model.node, style = model.typography.code) { code: String, lang: String?, _ ->
                extractedCode.value = code
                CodeEditor(project, code, lang)
            }
        }
        IconButton(
            onClick = { clipboard.setText(AnnotatedString(extractedCode.value)) },
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 4.dp)
        ) {
            Image(
                painter = loadIcon("/icons/copy.svg"),
                contentDescription = "Copy code",
                colorFilter = ColorFilter.tint(SonaTheme.colors.AiText),
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
private fun CodeEditor(project: Project, code: String, language: String?) {
    val fileType: FileType = remember(language) {
        language?.let {
            Language.findLanguageByID(it)?.associatedFileType
                ?: FileTypeManager.getInstance().getFileTypeByExtension(it)
        } ?: PlainTextFileType.INSTANCE
    }
    val document = remember(code) { EditorFactory.getInstance().createDocument(code) }
    SwingPanel(
        factory = { EditorTextField(document, project, fileType, true, false) },
        modifier = Modifier.fillMaxWidth()
    )
}
