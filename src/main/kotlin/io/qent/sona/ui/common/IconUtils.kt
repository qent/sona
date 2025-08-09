package io.qent.sona.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.intellij.openapi.util.IconLoader
import io.qent.sona.PluginStateFlow
import java.awt.image.BufferedImage
import javax.swing.Icon

@Composable
fun loadIcon(path: String): Painter {
    val icon = IconLoader.getIcon(path, PluginStateFlow::class.java)
    return remember(icon) { BitmapPainter(iconToImage(icon).toComposeImageBitmap()) }
}

private fun iconToImage(icon: Icon): BufferedImage {
    val image = BufferedImage(icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    icon.paintIcon(null, g, 0, 0)
    g.dispose()
    return image
}
