package io.qent.sona.ui.presets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import io.qent.sona.Strings
import io.qent.sona.core.presets.LlmProvider
import io.qent.sona.core.presets.LlmProviders
import io.qent.sona.core.presets.Preset
import io.qent.sona.core.state.State
import io.qent.sona.ui.common.DropdownSelector
import io.qent.sona.ui.common.SonaTheme
import io.qent.sona.ui.common.DeleteConfirmationDialog
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.ui.component.ActionButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import io.qent.sona.ui.common.TwoLineItem

@Composable
fun PresetsPanel(state: State.PresetsListState) {
    var deleteIndex by remember { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState()
    Box(
        Modifier
            .fillMaxSize()
            .background(SonaTheme.colors.Background)
            .padding(8.dp)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 56.dp)
        ) {
            itemsIndexed(state.presets) { idx, preset ->
                TwoLineItem(
                    title = preset.name,
                    subtitle = "${preset.provider.name} / ${preset.model}",
                    selected = idx == state.currentIndex,
                    onClick = { state.onSelectPreset(idx) },
                    onEdit = { state.onEditPreset(idx) },
                    onDelete = { deleteIndex = idx }
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(color = SonaTheme.colors.UserBubble)
                    .clickable { state.onAddPreset() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(Strings.addPreset, color = Color.White, fontSize = 12.sp)
            }
        }
        deleteIndex?.let { idx ->
            DeleteConfirmationDialog(
                text = Strings.deletePresetQuestion,
                onConfirm = {
                    state.onDeletePreset(idx)
                    deleteIndex = null
                },
                onCancel = { deleteIndex = null }
            )
        }
    }
}

@Composable
fun PresetsPanel(state: State.EditPresetState) {
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
            .padding(16.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            PresetForm(nameState, provider, {
                provider = it
                model = it.models.firstOrNull()?.name ?: ""
                apiState.setTextAndPlaceCursorAtEnd(it.defaultEndpoint)
            }, model, { model = it }, apiState, tokenState)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                ActionButton(onClick = state.onCancel, modifier = Modifier.weight(1f)) { Text(Strings.cancel) }
                Spacer(Modifier.width(8.dp))
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
                }, modifier = Modifier.weight(1f)) { Text(Strings.save) }
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
    tokenState: TextFieldState,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(Strings.name)
        Spacer(Modifier.height(6.dp))
        TextField(nameState, Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Text(Strings.provider)
        Spacer(Modifier.height(2.dp))
        DropdownSelector(
            items = LlmProviders.entries.map { it.name },
            selectedIndex = LlmProviders.entries.indexOfFirst { it.name == provider.name },
            expandUpwards = false,
            onSelect = { idx -> onProviderChange(LlmProviders.entries[idx]) },
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = SonaTheme.colors.Background,
            buttonModifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Text(Strings.model)
        Spacer(Modifier.height(2.dp))
        if (provider.models.isEmpty()) {
            val modelState = rememberTextFieldState(model)
            LaunchedEffect(model) { modelState.setTextAndPlaceCursorAtEnd(model) }
            TextField(modelState, Modifier.fillMaxWidth())
            LaunchedEffect(modelState.text) { onModelChange(modelState.text.toString()) }
        } else {
            DropdownSelector(
                items = provider.models.map { it.name },
                selectedIndex = provider.models.indexOfFirst { it.name == model }.coerceAtLeast(0),
                expandUpwards = false,
                onSelect = { idx -> onModelChange(provider.models[idx].name) },
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = SonaTheme.colors.Background,
                buttonModifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(Strings.apiUrl)
        Spacer(Modifier.height(6.dp))
        TextField(apiState, Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Text(Strings.token)
        Spacer(Modifier.height(6.dp))
        TextField(
            value = TextFieldValue("•".repeat(tokenState.text.length)),
            onValueChange = { value ->
                val input = value.text
                val old = tokenState.text.toString()
                val newValue: String? = when {
                    input.length < old.length -> old.dropLast(1)
                    input.length > old.length -> old + input.last()
                    else -> null
                }
                newValue?.let {
                    tokenState.setTextAndPlaceCursorAtEnd(it)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

