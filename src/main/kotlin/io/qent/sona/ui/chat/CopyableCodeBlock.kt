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
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.util.ui.ScrollUtil
import com.mikepenz.markdown.compose.components.MarkdownComponentModel
import com.mikepenz.markdown.compose.elements.MarkdownCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownCodeFence
import io.qent.sona.PluginStateFlow
import io.qent.sona.Strings
import io.qent.sona.services.PatchService
import io.qent.sona.util.isPatch
import com.intellij.openapi.components.service
import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener
import java.lang.Float.min
import javax.swing.*

/**
 * Renders a markdown code block with a copy button overlay.
 */
@Composable
fun CopyableCodeBlock(project: Project, model: MarkdownComponentModel, key: Any, fence: Boolean, onScrollOutside: ((Float) -> Unit)) {
    val clipboard = LocalClipboardManager.current

    if (fence) {
        MarkdownCodeFence(model.content, model.node, style = model.typography.code) { code: String, lang: String?, _ ->
            val isPatch = remember(code, lang) {
                isPatch(code, lang)
            }
            CodeEditor(project, code, lang, key, isPatch, onScrollOutside = onScrollOutside) {
                clipboard.setText(AnnotatedString(code))
            }
        }
    } else {
        MarkdownCodeBlock(model.content, model.node, style = model.typography.code) { code: String, lang: String?, _ ->
            val isPatch = remember(code) {
                isPatch(code)
            }
            CodeEditor(project, code, lang, key, isPatch, onScrollOutside = onScrollOutside) {
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
    key: Any,
    isPatch: Boolean,
    onScrollOutside: ((Float) -> Unit),
    onCopy: () -> Unit,
) {

    val editor = remember(key) {
        val fileType = language.associatedFileType
        val document = EditorFactory.getInstance().createDocument(code)
        val factory = EditorFactory.getInstance()
        (factory.createEditor(document, project, fileType, true) as EditorEx).apply {
            settings.apply {
                isLineNumbersShown     = true
                isFoldingOutlineShown  = true
                isIndentGuidesShown    = false
                isRightMarginShown     = false
                additionalColumnsCount = 0
                additionalLinesCount = 1
                isUseSoftWraps         = false
                isAdditionalPageAtBottom = false
            }

            installNestedScrollProxy(component, onScrollOutside)
        }
    }

    DisposableEffect(key1 = Unit) {
        onDispose {
            val swingInteropViewGroup = editor.component.parent?.parent
            swingInteropViewGroup?.parent?.remove(swingInteropViewGroup)
            EditorFactory.getInstance().releaseEditor(editor)
        }
    }

    ApplicationManager.getApplication().runWriteAction {
        editor.document.setText(code)
        editor.component.revalidate()
    }

    val heightPx = remember(editor.document.lineCount) {
        min((editor.document.lineCount + 1) * 23f, 200.dp.value)
    }

    SwingPanel(
        factory = {
            val editorComponent = editor.component

            val container = JPanel().apply {
                layout = OverlayLayout(this)
                isOpaque = false
                preferredSize = editorComponent.preferredSize
            }

            val copyIcon = IconLoader.getIcon("/icons/copy.svg", PluginStateFlow::class.java)
            val copyLabel = JLabel(copyIcon).apply {
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                toolTipText = Strings.copyCode
                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent?) {
                        onCopy()
                    }

                    override fun mousePressed(e: MouseEvent?) {
                        icon = IconLoader.getDisabledIcon(copyIcon)
                        repaint()
                    }
                    
                    override fun mouseReleased(e: MouseEvent?) {
                        icon = copyIcon
                        repaint()
                    }
                })
            }
            
            val patchLabel = if (isPatch) {
                val patchIcon = IconLoader.getIcon("/icons/applyPatch.svg", PluginStateFlow::class.java)
                JLabel(patchIcon).apply {
                    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    toolTipText = Strings.applyPatch
                    addMouseListener(object : MouseAdapter() {
                        override fun mouseClicked(e: MouseEvent?) {
                            project.service<PatchService>().showPatchDiff(code)
                        }
                        
                        override fun mousePressed(e: MouseEvent?) {
                            icon = IconLoader.getDisabledIcon(patchIcon)
                            repaint()
                        }
                        
                        override fun mouseReleased(e: MouseEvent?) {
                            icon = patchIcon
                            repaint()
                        }
                    })
                }
            } else null

            val overlayPanel = object : JPanel(null) {
                override fun doLayout() {
                    super.doLayout()
                    val x = width - 32
                    copyLabel.setBounds(x, 4, 28, 28)
                    patchLabel?.setBounds(x - 32, 4, 28, 28)
                }
            }.apply {
                isOpaque = false
                preferredSize = editorComponent.preferredSize
                add(copyLabel)
                if (patchLabel != null) add(patchLabel)
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
    SwingUtilities.invokeLater {
        val scrollPane = ScrollUtil.findScrollPane(editorComponent) ?: return@invokeLater


        scrollPane.addMouseWheelListener(object : MouseWheelListener {
            override fun mouseWheelMoved(e: MouseWheelEvent) {
                val bar = scrollPane.verticalScrollBar ?: return
                val atTop = bar.value == 0
                val atBottom = bar.value + bar.visibleAmount >= bar.maximum
                val scrollingUp = e.wheelRotation < 0
                val scrollingDown = e.wheelRotation > 0

                if ((scrollingUp && atTop) || (scrollingDown && atBottom)) {
                    onScrollOutside.invoke(e.preciseWheelRotation.toFloat() * 40f)
                }
            }
        })
    }
}

private val String?.associatedFileType get() = this?.let {
    Language.findLanguageByID(it)?.associatedFileType ?: FileTypeManager.getInstance().getFileTypeByExtension(it)
} ?: PlainTextFileType.INSTANCE
