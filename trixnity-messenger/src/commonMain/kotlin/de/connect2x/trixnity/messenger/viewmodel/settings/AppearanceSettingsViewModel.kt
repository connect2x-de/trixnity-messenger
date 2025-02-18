package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.ThemeMode
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.get

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
    val themeMode: StateFlow<ThemeMode>
    val isHighContrast: StateFlow<Boolean>
    val accentColor: StateFlow<Long?>
    val fontSize: StateFlow<Int>
    val controlsSize: StateFlow<Int>

    fun setThemeMode(themeMode: ThemeMode)
    fun toggleHighContrast()
    fun setAccentColor(accentColor: Long?)
    fun setFontSize(fontSize: Int)
    fun setControlsSize(controlsSize: Int)
    fun back()
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppearanceSettingsViewModelImpl(
    private val viewModelContext: ViewModelContext,
    private val onCloseAppearanceSettings: () -> Unit
) : ViewModelContext by viewModelContext, AppearanceSettingsViewModel {
    private val settings = get<MatrixMessengerSettingsHolder>()

    override val themeMode: StateFlow<ThemeMode> =
        settings.mapLatest { it.base.themeMode }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), settings.value.base.themeMode)
    override val isHighContrast: StateFlow<Boolean> =
        settings.mapLatest { it.base.isHighContrast }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), settings.value.base.isHighContrast)
    override val accentColor: StateFlow<Long?> =
        settings.mapLatest { it.base.accentColor }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), settings.value.base.accentColor)
    override val fontSize: StateFlow<Int> =
        settings.mapLatest { it.base.fontSize }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), settings.value.base.fontSize)
    override val controlsSize: StateFlow<Int> =
        settings.mapLatest { it.base.controlsSize }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), settings.value.base.controlsSize)

    private val backCallback = BackCallback {
        back()
    }

    init {
        backHandler.register(backCallback)
    }

    override fun setThemeMode(themeMode: ThemeMode) {
        coroutineScope.launch {
            settings.update<MatrixMessengerSettingsBase> {
                it.copy(themeMode = themeMode)
            }
        }
    }

    override fun toggleHighContrast() {
        coroutineScope.launch {
            settings.update<MatrixMessengerSettingsBase> {
                it.copy(isHighContrast = !it.isHighContrast)
            }
        }
    }

    override fun setAccentColor(accentColor: Long?) {
        coroutineScope.launch {
            settings.update<MatrixMessengerSettingsBase> {
                it.copy(accentColor = accentColor)
            }
        }
    }

    override fun setFontSize(fontSize: Int) {
        coroutineScope.launch {
            settings.update<MatrixMessengerSettingsBase> {
                it.copy(fontSize = fontSize)
            }
        }
    }

    override fun setControlsSize(controlsSize: Int) {
        coroutineScope.launch {
            settings.update<MatrixMessengerSettingsBase> {
                it.copy(controlsSize = controlsSize)
            }
        }
    }

    override fun back() {
        onCloseAppearanceSettings()
    }
}
