package io.qent.sona

import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import dev.langchain4j.model.anthropic.AnthropicChatModel
import io.qent.sona.core.StateProvider
import io.qent.sona.settings.PluginSettingsRepository
import io.qent.sona.chat.PluginChatRepository
import io.qent.sona.ui.PluginPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class PluginToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val settingsRepository = service<PluginSettingsRepository>()
        val chatRepository = service<PluginChatRepository>()
        val scope = CoroutineScope(Dispatchers.Default)
        val logic = StateProvider(settingsRepository, chatRepository, modelFactory = { settings ->
            AnthropicChatModel.builder()
                .apiKey(settings.apiKey)
                .baseUrl(settings.apiEndpoint)
                .modelName(settings.model)
                .build()
        }, scope = scope)
        val content = ContentFactory.getInstance().createContent(PluginPanel(logic).component(), null, false)
        toolWindow.contentManager.addContent(content)
    }
}