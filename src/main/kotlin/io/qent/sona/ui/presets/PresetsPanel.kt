package io.qent.sona.ui.presets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.qent.sona.core.LlmProvider
import io.qent.sona.core.Preset
import io.qent.sona.core.State
import io.qent.sona.ui.DropdownSelector
import io.qent.sona.ui.SonaTheme
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.ui.component.ActionButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField

@Composable
fun PresetsPanel(state: State.PresetsState) {
    val nameState = rememberTextFieldState(state.preset.name)
    var provider by remember { mutableStateOf(state.preset.provider) }
    var model by remember { mutableStateOf(state.preset.model) }
    val apiState = rememberTextFieldState(state.preset.apiEndpoint)
    val tokenState = rememberTextFieldState(state.preset.apiKey)

    LaunchedEffect(state.preset) {
        nameState.setTextAndPlaceCursorAtEnd(state.preset.name)
        provider = state.preset.provider
        model = state.preset.model
        apiState.setTextAndPlaceCursorAtEnd(state.preset.apiEndpoint)
        tokenState.setTextAndPlaceCursorAtEnd(state.preset.apiKey)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(SonaTheme.colors.Background)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            if (state.presets.isNotEmpty()) {
                Text(
                    "Models presets",
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
                    style = SonaTheme.markdownTypography.h5
                )
            }
            if (state.presets.isEmpty() || state.creating) {
                if (state.presets.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Start working with AI", style = SonaTheme.markdownTypography.h1)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "To get started, set up access to the LLM provider",
                            style = SonaTheme.markdownTypography.h4
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(
                            color = SonaTheme.colors.AiBubble,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(1.dp, SonaTheme.colors.BorderDefault, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        PresetForm(nameState, provider, {
                            provider = it
                            model = it.models.first().name
                            apiState.setTextAndPlaceCursorAtEnd(it.defaultEndpoint)
                        }, model, { model = it }, apiState, tokenState)
                        Spacer(Modifier.height(12.dp))
                        ActionButton(
                            onClick = {
                                state.onAddPreset(
                                    Preset(
                                        nameState.text.toString(),
                                        provider,
                                        apiState.text.toString(),
                                        model,
                                        tokenState.text.toString()
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                                .padding(top = 8.dp)
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                .padding(4.dp)
                        ) { Text("Save") }
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth()) {
                    DropdownSelector(
                        items = state.presets,
                        selectedIndex = state.currentIndex,
                        expandUpwards = false,
                        onSelect = { state.onSelectPreset(it) },
                        modifier = Modifier.weight(1f),
                        backgroundColor = SonaTheme.colors.Background,
                        buttonModifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.width(8.dp))
                    ActionButton(onClick = {
                        nameState.clearText()
                        tokenState.clearText()
                        state.onStartCreatePreset()
                    }) { Text("+") }
                    Spacer(Modifier.width(8.dp))
                    ActionButton(onClick = { state.onDeletePreset() }) { Text("\uD83D\uDDD1") }
                }
                Spacer(Modifier.height(8.dp))
                PresetForm(nameState, provider, {
                    provider = it
                    model = it.models.first().name
                    apiState.setTextAndPlaceCursorAtEnd(it.defaultEndpoint)
                }, model, { model = it }, apiState, tokenState)
                Spacer(Modifier.height(8.dp))
                ActionButton(onClick = {
                    state.onSave(
                        Preset(
                            nameState.text.toString(),
                            provider,
                            apiState.text.toString(),
                            model,
                            tokenState.text.toString()
                        )
                    )
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Save")
                }
            }
        }
    }
}

@OptIn(ExperimentalJewelApi::class)
@Composable
private fun PresetForm(
    nameState: TextFieldState,
    provider: LlmProvider,
    onProviderChange: (LlmProvider) -> Unit,
    model: String,
    onModelChange: (String) -> Unit,
    apiState: TextFieldState,
    tokenState: TextFieldState
) {
    Column(Modifier.fillMaxWidth()) {
        Text("Name")
        Spacer(Modifier.height(6.dp))
        TextField(nameState, Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Text("Provider")
        Spacer(Modifier.height(2.dp))
        DropdownSelector(
            items = LlmProvider.entries.map { it.name },
            selectedIndex = LlmProvider.entries.indexOf(provider),
            expandUpwards = false,
            onSelect = { idx -> onProviderChange(LlmProvider.entries[idx]) },
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = SonaTheme.colors.Background,
            buttonModifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Text("Model")
        Spacer(Modifier.height(2.dp))
        DropdownSelector(
            items = provider.models.map { it.name },
            selectedIndex = provider.models.indexOfFirst { it.name == model }.coerceAtLeast(0),
            expandUpwards = false,
            onSelect = { idx -> onModelChange(provider.models[idx].name) },
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = SonaTheme.colors.Background,
            buttonModifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Text("API URL")
        Spacer(Modifier.height(6.dp))
        TextField(apiState, Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Text("Token")
        Spacer(Modifier.height(6.dp))
        TextField(
            value = TextFieldValue("•".repeat(tokenState.text.length)),
            onValueChange = { value ->
                val input = value.text
                val old = tokenState.text.toString()
                val newValue: String? = when {
                    input.length < old.length -> old.dropLast(old.length - input.length)
                    input.length > old.length -> old + input.substring(old.length)
                    else -> null
                }
                newValue?.let { tokenState.setTextAndPlaceCursorAtEnd(it) }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
