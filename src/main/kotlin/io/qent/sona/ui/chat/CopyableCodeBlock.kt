package io.qent.sona.ui.chat

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
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
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.JBColor
import com.intellij.util.ui.ScrollUtil
import com.mikepenz.markdown.compose.components.MarkdownComponentModel
import com.mikepenz.markdown.compose.elements.MarkdownCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownCodeFence
import io.qent.sona.PluginStateFlow
import io.qent.sona.Strings
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.lang.Float.min
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.OverlayLayout

/**
 * Renders a markdown code block with a copy button overlay.
 */
@Composable
fun CopyableCodeBlock(project: Project, model: MarkdownComponentModel, fence: Boolean, onScrollOutside: ((Float) -> Unit)) {
    val clipboard = LocalClipboardManager.current

    if (fence) {
        MarkdownCodeFence(model.content, model.node, style = model.typography.code) { code: String, lang: String?, _ ->
            CodeEditor(project, code, lang, onScrollOutside = onScrollOutside) {
                clipboard.setText(AnnotatedString(code))
            }
        }
    } else {
        MarkdownCodeBlock(model.content, model.node, style = model.typography.code) { code: String, lang: String?, _ ->
            CodeEditor(project, code, lang, onScrollOutside = onScrollOutside) {
                clipboard.setText(AnnotatedString(code))
            }
        }
    }
}

@Composable
fun CodeEditor(
    project: Project,
    code: String,
    language: String?,
    parentDisposable: Disposable = project,
    onScrollOutside: ((Float) -> Unit),
    onCopy: () -> Unit,
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
        installNestedScrollProxy(editor.component, onScrollOutside)

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
        factory = {
            val editorComponent = editor.component

            val container = JPanel().apply {
                layout = OverlayLayout(this)
                isOpaque = false
                preferredSize = editorComponent.preferredSize
            }

            // панель поверх редактора для кнопки копирования
            val copyIcon = IconLoader.getIcon("/icons/copy.svg", PluginStateFlow::class.java)
            val label = JLabel(copyIcon).apply {
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                toolTipText = Strings.copyCode
                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent?) {
                        onCopy()
                    }
                })
            }
            val overlayPanel = object : JPanel(null) {
                override fun doLayout() {
                    super.doLayout()
                    // label всегда в правом верхнем углу с отступом 8px
                    label.setBounds(width - 32, 4, 28, 28)
                }
            }.apply {
                isOpaque = false
                preferredSize = editorComponent.preferredSize
                add(label)
            }

            container.add(overlayPanel)
            container.add(editorComponent)

            return@SwingPanel container
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(heightPx.dp)
    )
}

private fun installNestedScrollProxy(editorComponent: JComponent, onScrollOutside: (Float) -> Unit) {
    javax.swing.SwingUtilities.invokeLater {
        val scrollPane = ScrollUtil.findScrollPane(editorComponent) ?: return@invokeLater


        scrollPane.addMouseWheelListener(object : java.awt.event.MouseWheelListener {
            override fun mouseWheelMoved(e: java.awt.event.MouseWheelEvent) {
                val bar = scrollPane.verticalScrollBar ?: return
                val atTop = bar.value == 0
                val atBottom = bar.value + bar.visibleAmount >= bar.maximum
                val scrollingUp = e.wheelRotation < 0
                val scrollingDown = e.wheelRotation > 0

                if ((scrollingUp && atTop) || (scrollingDown && atBottom)) {
                    // проксировать событие наружу
                    onScrollOutside.invoke(e.preciseWheelRotation.toFloat() * 40f)
                }
            }
        })
    }
}
