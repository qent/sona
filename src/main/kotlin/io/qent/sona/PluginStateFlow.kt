package io.qent.sona

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import dev.langchain4j.model.anthropic.AnthropicChatModel
import io.qent.sona.core.State
import io.qent.sona.core.StateProvider
import io.qent.sona.core.Tools
import io.qent.sona.repositories.PluginChatRepository
import io.qent.sona.repositories.PluginRolesRepository
import io.qent.sona.repositories.PluginSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*


@Service(Service.Level.PROJECT)
class PluginStateFlow(private val project: Project) : StateFlow<State> {

    private val settingsRepository = service<PluginSettingsRepository>()
    private val chatRepository = service<PluginChatRepository>()
    private val rolesRepository = service<PluginRolesRepository>()
    private val scope = CoroutineScope(Dispatchers.Default)
    private val stateProvider = StateProvider(
        settingsRepository,
        chatRepository,
        rolesRepository,
        modelFactory = { settings ->
            AnthropicChatModel.builder()
                .apiKey(settings.apiKey)
                .baseUrl(settings.apiEndpoint)
                .modelName(settings.model)
                .build()
        },
        tools = object : Tools {
            override fun getFocusedFileText(): String? {
                return FileEditorManager.getInstance(project).selectedTextEditor?.document?.text
            }
        }, scope = scope
    )

    init {
        val trustAll: Array<TrustManager> = arrayOf(object : X509TrustManager {
            override fun checkClientTrusted(c: Array<X509Certificate?>?, a: String?) = Unit
            override fun checkServerTrusted(c: Array<X509Certificate?>?, a: String?) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate?>? = arrayOfNulls(0)
        })
        val sc = SSLContext.getInstance("SSL")
        sc.init(null, trustAll, SecureRandom())
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
        HttpsURLConnection.setDefaultHostnameVerifier { h: String?, s: SSLSession? -> true }
    }

    override val value get() = stateProvider.state.value

    override val replayCache get() = stateProvider.state.replayCache

    override suspend fun collect(collector: FlowCollector<State>): Nothing {
        stateProvider.state.collect(collector)
    }
}