package io.qent.sona.settings

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.input.rememberTextFieldState
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import io.qent.sona.Strings
import io.qent.sona.repositories.PluginSettingsRepository
import io.qent.sona.services.ThemeService
import io.qent.sona.ui.common.SonaTheme
import org.jetbrains.jewel.bridge.JewelComposePanel
import org.jetbrains.jewel.ui.component.Checkbox
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField

class PluginSettingsConfigurable : Configurable {

    private val repo = service<PluginSettingsRepository>()
    private var currentIgnoreHttpsErrors = repo.state.ignoreHttpsErrors
    private var currentCacheSystemPrompts = repo.state.cacheSystemPrompts
    private var currentCacheToolDescriptions = repo.state.cacheToolDescriptions
    private var currentApiRetries = repo.state.apiRetries

    override fun createComponent() = JewelComposePanel {
        val themeService = service<ThemeService>()
        val dark by themeService.isDark.collectAsState()
        var ignoreHttps by remember { mutableStateOf(currentIgnoreHttpsErrors) }
        var cacheSystemPrompts by remember { mutableStateOf(currentCacheSystemPrompts) }
        var cacheToolDescriptions by remember { mutableStateOf(currentCacheToolDescriptions) }
        val apiRetriesState = rememberTextFieldState(currentApiRetries.toString())
        LaunchedEffect(ignoreHttps) { currentIgnoreHttpsErrors = ignoreHttps }
        LaunchedEffect(cacheSystemPrompts) { currentCacheSystemPrompts = cacheSystemPrompts }
        LaunchedEffect(cacheToolDescriptions) { currentCacheToolDescriptions = cacheToolDescriptions }
        LaunchedEffect(apiRetriesState.text) {
            currentApiRetries = apiRetriesState.text.toString().toIntOrNull() ?: 0
        }
        SonaTheme(dark = dark) {
            Column(modifier = Modifier.width(600.dp).padding(16.dp)) {
                Text(Strings.pluginSettings)
                Spacer(Modifier.height(8.dp))
                Row {
                    Checkbox(checked = ignoreHttps, onCheckedChange = { ignoreHttps = it })
                    Spacer(Modifier.width(8.dp))
                      Text(Strings.ignoreHttpsErrors)
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    TextField(apiRetriesState, Modifier.width(60.dp))
                    Spacer(Modifier.width(8.dp))
                      Text(Strings.apiRetries)
                }
                Spacer(Modifier.height(16.dp))
                Text(Strings.anthropicSettings)
                Spacer(Modifier.height(8.dp))
                Row {
                    Checkbox(checked = cacheSystemPrompts, onCheckedChange = { cacheSystemPrompts = it })
                    Spacer(Modifier.width(8.dp))
                      Text(Strings.cacheSystemPrompts)
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    Checkbox(checked = cacheToolDescriptions, onCheckedChange = { cacheToolDescriptions = it })
                    Spacer(Modifier.width(8.dp))
                      Text(Strings.cacheToolDescriptions)
                }
            }
        }
    }

    override fun isModified(): Boolean {
        val saved = repo.state
        return currentIgnoreHttpsErrors != saved.ignoreHttpsErrors ||
            currentCacheSystemPrompts != saved.cacheSystemPrompts ||
            currentCacheToolDescriptions != saved.cacheToolDescriptions ||
            currentApiRetries != saved.apiRetries
    }

    override fun apply() {
        repo.loadState(
            PluginSettingsRepository.PluginSettingsState(
                currentIgnoreHttpsErrors,
                currentCacheSystemPrompts,
                currentCacheToolDescriptions,
                currentApiRetries,
            )
        )
    }

    override fun getDisplayName(): String = "Sona"
}

