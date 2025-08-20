package io.qent.sona.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.qent.sona.Strings
import io.qent.sona.core.state.State.ChatState
import io.qent.sona.core.model.cost
import io.qent.sona.ui.common.SonaTheme
import org.jetbrains.jewel.ui.component.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun ChatHeader(state: ChatState) {
    var isExpanded by remember { mutableStateOf(false) }

    val preset = state.presets.presets.getOrNull(state.presets.active)
    val model = preset?.provider?.models?.find { it.name == preset.model }
    val totalCost = state.totalTokenUsage.cost(model)
    val lastCost = state.lastTokenUsage.cost(model)
    val contextTokens = state.totalTokenUsage.outputTokens + state.totalTokenUsage.inputTokens
    val maxContext = model?.maxContextTokens ?: 0
    val rawProgress = if (maxContext > 0) (contextTokens.toFloat() / maxContext.toFloat()).coerceIn(0f, 1f) else 0f
    val progress by animateFloatAsState(targetValue = rawProgress, animationSpec = tween(350), label = "contextProgress")

    val dark = isSystemInDarkTheme()
    val cardBorder = if (dark) Color(0xFF2A2F36) else Color(0xFFE7EAF0)
    val chipBg = if (dark) Color(0xFF122315) else Color(0xFFE8F5E9)
    val chipFg = if (dark) Color(0xFF8EE89B) else Color(0xFF256C2B)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SonaTheme.colors.InputBackground)
            .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
            .clickable { isExpanded = !isExpanded }
            .padding(4.dp)
            .padding(horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(4.dp))

            ContextProgressBar(
                progress = progress,
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            InfoChip(
                text = formatCost(totalCost),
                bg = chipBg,
                fg = chipFg
            )

            Spacer(modifier = Modifier.width(6.dp))

            Chevron(isExpanded = isExpanded)
        }

        if (isExpanded) {
            Spacer(modifier = Modifier.height(12.dp))
            ExpandedTokenDetails(
                state = state,
                totalCost = totalCost,
                lastCost = lastCost,
                contextTokens = contextTokens,
                maxContext = maxContext
            )
        }
    }
}

@Composable
private fun ExpandedTokenDetails(
    state: ChatState,
    totalCost: Double,
    lastCost: Double,
    contextTokens: Int,
    maxContext: Int
) {
    val dark = isSystemInDarkTheme()
    val divider = if (dark) Color(0xFF2A2F36) else Color(0xFFE7EAF0)
    val accent = Color(0xFF4CAF50)

    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val contextPercent = if (maxContext > 0) contextTokens * 100 / maxContext else 0
        MetricRow(Strings.context, "$contextTokens / $maxContext ($contextPercent%)")

        Hairline(divider)

        SectionHeading(Strings.totalTokenUsage)
        MetricRow(Strings.output, "${state.totalTokenUsage.outputTokens}")
        MetricRow(Strings.input, "${state.totalTokenUsage.inputTokens}")
        MetricRow(Strings.cacheCreated, "${state.totalTokenUsage.cacheCreationInputTokens}")
        MetricRow(Strings.cacheRead, "${state.totalTokenUsage.cacheReadInputTokens}")
        MetricRow(Strings.totalCost, formatCost(totalCost), valueColor = accent)

        Hairline(divider)

        SectionHeading(Strings.lastRequest)
        MetricRow(Strings.output, "${state.lastTokenUsage.outputTokens}")
        MetricRow(Strings.input, "${state.lastTokenUsage.inputTokens}")
        MetricRow(Strings.cacheCreated, "${state.lastTokenUsage.cacheCreationInputTokens}")
        MetricRow(Strings.cacheRead, "${state.lastTokenUsage.cacheReadInputTokens}")
        MetricRow(Strings.requestCost, formatCost(lastCost), valueColor = accent)
    }
}

@Composable
private fun MetricRow(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            modifier = Modifier.width(140.dp),
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            color = valueColor,
            modifier = Modifier.weight(1f),
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SectionHeading(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
        style = SonaTheme.markdownTypography.h5,
        // Make headings slightly bolder for hierarchy
        // Fallback if typography doesn't carry weight
        // (Jewel's Text ignores weight if not supported)
    )
}

@Composable
private fun Hairline(color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color.copy(alpha = 0.6f))
    )
}

@Composable
private fun Chevron(isExpanded: Boolean) {
    val rotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, animationSpec = tween(200), label = "chevronRotation")
    Text(
        text = "▾",
        modifier = Modifier
            .size(18.dp)
            .rotate(rotation),
    )
}

@Composable
private fun InfoChip(text: String, bg: Color = Color(0xFFE8F5E9), fg: Color = Color(0xFF256C2B)) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = text, color = fg)
    }
}

@Composable
private fun ContextProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val track = Color(0x33445566)
    val brush = Brush.horizontalGradient(
        colors = listOf(Color(0xFF6EA8FE), Color(0xFFB388FF))
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(track)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .fillMaxHeight()
                .clip(RoundedCornerShape(3.dp))
                .background(brush)
        )
    }
}

private fun formatCost(cost: Double) = "$" + String.format("%.4f", cost)