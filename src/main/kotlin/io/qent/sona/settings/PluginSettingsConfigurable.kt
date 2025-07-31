package io.qent.sona.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.components.service
import javax.swing.JComponent
import org.jetbrains.jewel.bridge.JewelComposePanel
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField

class PluginSettingsConfigurable : Configurable {

    private val repo = service<PluginSettingsRepository>()

    private var currentApiKey = repo.state.apiKey
    private var currentApiEndpoint = repo.state.apiEndpoint
    private var currentModel = repo.state.model

    override fun createComponent(): JComponent {
        return JewelComposePanel {
            val apiKey = rememberTextFieldState(currentApiKey)
            val apiEndpoint = rememberTextFieldState(currentApiEndpoint)
            val model = rememberTextFieldState(currentModel)

            LaunchedEffect(apiKey.text) { currentApiKey = apiKey.text.toString() }
            LaunchedEffect(apiEndpoint.text) { currentApiEndpoint = apiEndpoint.text.toString() }
            LaunchedEffect(model.text) { currentModel = model.text.toString() }

            Column(Modifier.width(600.dp).padding(16.dp)) {
                Text("API Key")
                TextField(
                    apiKey,
                    Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text("Endpoint")
                TextField(apiEndpoint, Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("Model")
                TextField(model, Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    override fun isModified(): Boolean {
        val savedState = repo.state
        return currentApiKey != savedState.apiKey
                || currentApiEndpoint != savedState.apiEndpoint
                || currentModel != savedState.model
    }

    override fun apply() {
        repo.loadState(
            PluginSettingsRepository.PluginSettingsState(
                currentApiKey,
                currentApiEndpoint,
                currentModel
            )
        )
    }

    override fun getDisplayName(): String = "Sona"
}
