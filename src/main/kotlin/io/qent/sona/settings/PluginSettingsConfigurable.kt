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
    private var currentCacheSystemPrompts = repo.state.cacheSystemPrompts
    private var currentCacheToolDescriptions = repo.state.cacheToolDescriptions

    override fun createComponent() = JewelComposePanel {
        val themeService = service<ThemeService>()
        val dark by themeService.isDark.collectAsState()
        var ignoreHttps by remember { mutableStateOf(currentIgnoreHttpsErrors) }
        var cacheSystemPrompts by remember { mutableStateOf(currentCacheSystemPrompts) }
        var cacheToolDescriptions by remember { mutableStateOf(currentCacheToolDescriptions) }
        LaunchedEffect(ignoreHttps) { currentIgnoreHttpsErrors = ignoreHttps }
        LaunchedEffect(cacheSystemPrompts) { currentCacheSystemPrompts = cacheSystemPrompts }
        LaunchedEffect(cacheToolDescriptions) { currentCacheToolDescriptions = cacheToolDescriptions }
        SonaTheme(dark = dark) {
            Column(modifier = Modifier.width(600.dp).padding(16.dp)) {
                Text("Plugin Settings")
                Spacer(Modifier.height(8.dp))
                Row {
                    Checkbox(checked = ignoreHttps, onCheckedChange = { ignoreHttps = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Ignore HTTPS errors")
                }
                Spacer(Modifier.height(16.dp))
                Text("Anthropic Settings")
                Spacer(Modifier.height(8.dp))
                Row {
                    Checkbox(checked = cacheSystemPrompts, onCheckedChange = { cacheSystemPrompts = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Cache system prompts")
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    Checkbox(checked = cacheToolDescriptions, onCheckedChange = { cacheToolDescriptions = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Cache tool descriptions")
                }
            }
        }
    }

    override fun isModified(): Boolean {
        val saved = repo.state
        return currentIgnoreHttpsErrors != saved.ignoreHttpsErrors ||
            currentCacheSystemPrompts != saved.cacheSystemPrompts ||
            currentCacheToolDescriptions != saved.cacheToolDescriptions
    }

    override fun apply() {
        repo.loadState(
            PluginSettingsRepository.PluginSettingsState(
                currentIgnoreHttpsErrors,
                currentCacheSystemPrompts,
                currentCacheToolDescriptions,
            )
        )
    }

    override fun getDisplayName(): String = "Sona"
}

