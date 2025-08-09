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

@Composable
fun ChatHeader(state: ChatState) {
    var isExpanded by remember { mutableStateOf(false) }

    val preset = state.presets.presets.getOrNull(state.presets.active)
    val model = preset?.provider?.models?.find { it.name == preset.model }
    val totalCost = state.totalTokenUsage.cost(model)
    val lastCost = state.lastTokenUsage.cost(model)
    val contextTokens = state.lastTokenUsage.outputTokens + state.lastTokenUsage.inputTokens
    val maxContext = model?.maxContextTokens ?: 0
    val contextPercent = if (maxContext > 0) (contextTokens.toFloat() / maxContext.toFloat()).coerceAtMost(1f) else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { isExpanded = !isExpanded }
    ) {
        if (isExpanded) {
            ExpandedTokenDetails(state, totalCost, lastCost, contextTokens, maxContext)
        } else {
            CollapsedTokenSummary(totalCost, contextPercent)
        }
    }
}

@Composable
private fun CollapsedTokenSummary(totalCost: Double, contextPercent: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Прогресс-бар контекста
        Box(
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.Gray)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(contextPercent)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF2196F3)) // Голубой цвет
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Общая стоимость зеленым цветом
        Text(
            text = formatCost(totalCost),
            color = Color(0xFF4CAF50) // Зеленый цвет
        )
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
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          val contextPercent = if (maxContext > 0) contextTokens * 100 / maxContext else 0
          TwoColumnRow(Strings.context, "$contextTokens / $maxContext ($contextPercent%)")

        Spacer(modifier = Modifier.height(8.dp))

          Text(Strings.totalTokenUsage, modifier = Modifier.padding(bottom = 4.dp), style = SonaTheme.markdownTypography.h5)
          TwoColumnRow(Strings.output, "${state.totalTokenUsage.outputTokens}")
          TwoColumnRow(Strings.input, "${state.totalTokenUsage.inputTokens}")
          TwoColumnRow(Strings.cacheCreated, "${state.totalTokenUsage.cacheCreationInputTokens}")
          TwoColumnRow(Strings.cacheRead, "${state.totalTokenUsage.cacheReadInputTokens}")
          TwoColumnRow(Strings.totalCost, formatCost(totalCost), Color(0xFF4CAF50))

        Spacer(modifier = Modifier.height(8.dp))

          Text(Strings.lastRequest, modifier = Modifier.padding(bottom = 4.dp), style = SonaTheme.markdownTypography.h5)
          TwoColumnRow(Strings.output, "${state.lastTokenUsage.outputTokens}")
          TwoColumnRow(Strings.input, "${state.lastTokenUsage.inputTokens}")
          TwoColumnRow(Strings.cacheCreated, "${state.lastTokenUsage.cacheCreationInputTokens}")
          TwoColumnRow(Strings.cacheRead, "${state.lastTokenUsage.cacheReadInputTokens}")
          TwoColumnRow(Strings.requestCost, formatCost(lastCost), Color(0xFF4CAF50))
    }
}

@Composable
private fun TwoColumnRow(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value,
            color = valueColor,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun formatCost(cost: Double) = "$" + String.format("%.4f", cost)