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
    private var currentAnswerInEnglish = repo.state.answerInEnglish
    private var currentIgnoreHttpsErrors = repo.state.ignoreHttpsErrors
    private var currentEnablePluginLogging = repo.state.enablePluginLogging
    private var currentCacheSystemPrompts = repo.state.cacheSystemPrompts
    private var currentCacheToolDescriptions = repo.state.cacheToolDescriptions
    private var currentApiRetries = repo.state.apiRetries
    private var currentUseSearchAgent = repo.state.useSearchAgent

    override fun createComponent() = JewelComposePanel {
        val themeService = service<ThemeService>()
        val dark by themeService.isDark.collectAsState()
        var answerInEnglish by remember { mutableStateOf(currentAnswerInEnglish) }
        var ignoreHttps by remember { mutableStateOf(currentIgnoreHttpsErrors) }
        var enableLogging by remember { mutableStateOf(currentEnablePluginLogging) }
        var cacheSystemPrompts by remember { mutableStateOf(currentCacheSystemPrompts) }
        var cacheToolDescriptions by remember { mutableStateOf(currentCacheToolDescriptions) }
        var useSearchAgent by remember { mutableStateOf(currentUseSearchAgent) }
        val apiRetriesState = rememberTextFieldState(currentApiRetries.toString())
        LaunchedEffect(answerInEnglish) { currentAnswerInEnglish = answerInEnglish }
        LaunchedEffect(ignoreHttps) { currentIgnoreHttpsErrors = ignoreHttps }
        LaunchedEffect(enableLogging) { currentEnablePluginLogging = enableLogging }
        LaunchedEffect(cacheSystemPrompts) { currentCacheSystemPrompts = cacheSystemPrompts }
        LaunchedEffect(cacheToolDescriptions) { currentCacheToolDescriptions = cacheToolDescriptions }
        LaunchedEffect(useSearchAgent) { currentUseSearchAgent = useSearchAgent }
        LaunchedEffect(apiRetriesState.text) {
            currentApiRetries = apiRetriesState.text.toString().toIntOrNull() ?: 0
        }
        SonaTheme(dark = dark) {
            Column(modifier = Modifier.width(600.dp).padding(16.dp)) {
                Text(Strings.pluginSettings)
                Spacer(Modifier.height(8.dp))
                Row {
                    Checkbox(checked = answerInEnglish, onCheckedChange = { answerInEnglish = it })
                    Spacer(Modifier.width(8.dp))
                      Text(Strings.answerInEnglish)
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    Checkbox(checked = ignoreHttps, onCheckedChange = { ignoreHttps = it })
                    Spacer(Modifier.width(8.dp))
                      Text(Strings.ignoreHttpsErrors)
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    Checkbox(checked = enableLogging, onCheckedChange = { enableLogging = it })
                    Spacer(Modifier.width(8.dp))
                      Text(Strings.enablePluginLogging)
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    Checkbox(checked = useSearchAgent, onCheckedChange = { useSearchAgent = it })
                    Spacer(Modifier.width(8.dp))
                      Text(Strings.useSearchAgent)
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
        return currentAnswerInEnglish != saved.answerInEnglish ||
            currentIgnoreHttpsErrors != saved.ignoreHttpsErrors ||
            currentEnablePluginLogging != saved.enablePluginLogging ||
            currentCacheSystemPrompts != saved.cacheSystemPrompts ||
            currentCacheToolDescriptions != saved.cacheToolDescriptions ||
            currentApiRetries != saved.apiRetries ||
            currentUseSearchAgent != saved.useSearchAgent
    }

    override fun apply() {
        repo.loadState(
            PluginSettingsRepository.PluginSettingsState(
                currentIgnoreHttpsErrors,
                currentEnablePluginLogging,
                currentCacheSystemPrompts,
                currentCacheToolDescriptions,
                currentApiRetries,
                currentAnswerInEnglish,
                currentUseSearchAgent,
            )
        )
    }

    override fun getDisplayName(): String = "Sona"
}

