package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import kotlinx.coroutines.flow.MutableStateFlow

interface AppearanceSettingsViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        onCloseAppearanceSettings: () -> Unit
    ): AppearanceSettingsViewModel {
        return AppearanceSettingsViewModelImpl(viewModelContext, onCloseAppearanceSettings)
    }

    companion object : AppearanceSettingsViewModelFactory
}

interface AppearanceSettingsViewModel {
    val isDarkTheme: MutableStateFlow<Boolean?>
    val isHighContrast: MutableStateFlow<Boolean>
    fun back()
}

class AppearanceSettingsViewModelImpl(
    viewModelContext: ViewModelContext,
    val onCloseAppearanceSettings: () -> Unit
) : ViewModelContext by viewModelContext, AppearanceSettingsViewModel {
    override val isDarkTheme: MutableStateFlow<Boolean?> =
        MutableStateFlow(null)
    override val isHighContrast: MutableStateFlow<Boolean> =
        MutableStateFlow(false)

    private val backCallback = BackCallback {
        back()
    }

    init {
        backHandler.register(backCallback)
    }

    override fun back() {
        onCloseAppearanceSettings()
    }
}
