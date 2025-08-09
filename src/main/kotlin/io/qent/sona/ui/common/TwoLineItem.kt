package io.qent.sona.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.ui.component.Text

@Composable
fun TwoLineItem(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) SonaTheme.colors.UserBubble else SonaTheme.colors.AiBubble)
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = SonaTheme.colors.AiText, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = SonaTheme.colors.BackgroundText, fontSize = 12.sp)
        }
        onEdit?.let {
            Image(
                painter = loadIcon("/icons/edit.svg"),
                contentDescription = null,
                colorFilter = ColorFilter.tint(SonaTheme.colors.AiText),
                modifier = Modifier
                    .size(16.dp)
                    .clickable(onClick = it)
            )
        }
        if (onEdit != null && onDelete != null) {
            Spacer(Modifier.width(12.dp))
        }
        onDelete?.let {
            Image(
                painter = loadIcon("/icons/trash.svg"),
                contentDescription = null,
                colorFilter = ColorFilter.tint(SonaTheme.colors.AiText),
                modifier = Modifier
                    .size(16.dp)
                    .clickable(onClick = it)
            )
        }
    }
}
