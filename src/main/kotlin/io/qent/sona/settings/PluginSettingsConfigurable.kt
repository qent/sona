package io.qent.sona.settings

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import io.qent.sona.repositories.PluginSettingsRepository
import io.qent.sona.services.ThemeService
import io.qent.sona.ui.SonaTheme
import org.jetbrains.jewel.bridge.JewelComposePanel
import org.jetbrains.jewel.ui.component.Checkbox
import org.jetbrains.jewel.ui.component.Text

class PluginSettingsConfigurable : Configurable {

    private val repo = service<PluginSettingsRepository>()
    private var currentIgnoreHttpsErrors = repo.state.ignoreHttpsErrors

    override fun createComponent() = JewelComposePanel {
        val themeService = service<ThemeService>()
        val dark by themeService.isDark.collectAsState()
        var checked by remember { mutableStateOf(currentIgnoreHttpsErrors) }
        LaunchedEffect(checked) { currentIgnoreHttpsErrors = checked }
        SonaTheme(dark = dark) {
            Column(modifier = Modifier.width(600.dp).padding(16.dp)) {
                Row {
                    Checkbox(checked = checked, onCheckedChange = { checked = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Ignore HTTPS errors")
                }
            }
        }
    }

    override fun isModified(): Boolean {
        val saved = repo.state
        return currentIgnoreHttpsErrors != saved.ignoreHttpsErrors
    }

    override fun apply() {
        repo.loadState(PluginSettingsRepository.PluginSettingsState(currentIgnoreHttpsErrors))
    }

    override fun getDisplayName(): String = "Sona"
}

