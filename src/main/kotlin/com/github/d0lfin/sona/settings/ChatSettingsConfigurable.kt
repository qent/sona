package com.github.d0lfin.sona.settings

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.d0lfin.sona.logic.ChatSettings
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.components.service
import javax.swing.JComponent
import org.jetbrains.jewel.bridge.JewelComposePanel
import org.jetbrains.jewel.ui.component.Button
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField

class ChatSettingsConfigurable : Configurable {
    private var panel: JComponent? = null
    override fun createComponent(): JComponent {
        val repo = service<ChatSettingsRepositoryImpl>()
        panel = JewelComposePanel {
            val state: MutableState<String> = remember { mutableStateOf(repo.state.apiKey) }
            val url: MutableState<String> = remember { mutableStateOf(repo.state.baseUrl) }
            val model: MutableState<String> = remember { mutableStateOf(repo.state.model) }
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("API Token")
                TextField(state.value, onValueChange = { state.value = it })
                Spacer(Modifier.height(8.dp))
                Text("Endpoint")
                TextField(url.value, onValueChange = { url.value = it })
                Spacer(Modifier.height(8.dp))
                Text("Model")
                TextField(model.value, onValueChange = { model.value = it })
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    repo.state = ChatSettingsRepositoryImpl.State(state.value, url.value, model.value)
                }) { Text("Save") }
            }
        }
        return panel!!
    }

    override fun isModified(): Boolean = false
    override fun apply() {}
    override fun getDisplayName(): String = "Sona Chat"
}
