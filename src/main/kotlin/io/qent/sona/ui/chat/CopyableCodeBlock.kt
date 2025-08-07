package io.qent.sona.ui.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.mikepenz.markdown.compose.components.MarkdownComponentModel
import com.mikepenz.markdown.compose.elements.MarkdownCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownCodeFence
import io.qent.sona.ui.SonaTheme
import org.jetbrains.jewel.ui.component.IconButton
import java.lang.Float.min

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
fun CodeEditor(
    project: Project,
    code: String,
    language: String?,
    parentDisposable: Disposable = project      // можно передать свой узел
) {
    val (editor, editorNode) = remember(project, code, language) {
        val fileType = language?.let {
            Language.findLanguageByID(it)?.associatedFileType
                ?: FileTypeManager.getInstance().getFileTypeByExtension(it)
        } ?: PlainTextFileType.INSTANCE

        val document = EditorFactory.getInstance().createDocument(code)
        val editor = EditorFactory.getInstance()
            .createEditor(document, project, fileType, /* isViewer = */ true)

        (editor as? EditorEx)?.settings?.apply {
            isLineNumbersShown     = false
            isFoldingOutlineShown  = false
            isIndentGuidesShown    = false
            isRightMarginShown     = false
            additionalColumnsCount = 0
            isUseSoftWraps         = false
        }

        val node = Disposer.newDisposable("CodeEditorDisposable")
        Disposer.register(node) { EditorFactory.getInstance().releaseEditor(editor) }
        Disposer.register(parentDisposable, node)

        // Install nested scroll proxy for the editor component
        installNestedScrollProxy(editor.component)

        editor to node
    }

    DisposableEffect(Unit) {
        onDispose {
            ApplicationManager.getApplication().invokeLater(
                { Disposer.dispose(editorNode) },
                ModalityState.any()
            )
        }
    }

    val heightPx = remember(editor) {
        min(editor.document.lineCount * 25f, 200.dp.value)
    }

    SwingPanel(
        factory = { editor.component },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(heightPx.dp)
    )
}

private fun installNestedScrollProxy(editorComponent: java.awt.Component) {
    javax.swing.SwingUtilities.invokeLater {
        val scrollPane = javax.swing.SwingUtilities.getAncestorOfClass(
            javax.swing.JScrollPane::class.java, editorComponent
        ) as? javax.swing.JScrollPane ?: return@invokeLater

        scrollPane.addMouseWheelListener(object : java.awt.event.MouseWheelListener {
            override fun mouseWheelMoved(e: java.awt.event.MouseWheelEvent) {
                val bar = scrollPane.verticalScrollBar ?: return
                val atTop = bar.value == 0
                val atBottom = bar.value + bar.visibleAmount >= bar.maximum
                val scrollingUp = e.wheelRotation < 0
                val scrollingDown = e.wheelRotation > 0

                if ((scrollingUp && atTop) || (scrollingDown && atBottom)) {
                    // проксировать событие наружу
                    editorComponent.parent?.dispatchEvent(e)
                }
            }
        })
    }
}
