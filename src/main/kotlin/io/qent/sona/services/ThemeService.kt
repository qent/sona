package io.qent.sona.services

import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.swing.UIManager

class ThemeService {
    private val _isDark = MutableStateFlow(UIUtil.isUnderDarcula())
    val isDark: StateFlow<Boolean> get() = _isDark

    init {
        UIManager.addPropertyChangeListener {
            if (it.propertyName == "lookAndFeel") {
                _isDark.value = UIUtil.isUnderDarcula()
            }
        }
    }
}
