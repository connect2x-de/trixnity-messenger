package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.MatrixMessengerSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.ThemeMode
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.util.BackCallback
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
    val isFocusHighlighting: StateFlow<Boolean>
    val accentColor: StateFlow<Long?>
    val fontSize: StateFlow<Float?>
    val displaySize: StateFlow<Float?>
    val applySystemSizes: StateFlow<Boolean>

    fun setThemeMode(themeMode: ThemeMode)
    fun toggleHighContrast()
    fun toggleFocusHighlighting()
    fun setAccentColor(accentColor: Long?)
    fun setFontSize(fontSize: Float?)
    fun setDisplaySize(controlsSize: Float?)
    fun toggleApplySystemSizes()
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
    override val isFocusHighlighting: StateFlow<Boolean> =
        settings.mapLatest { it.base.isFocusHighlighting }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), settings.value.base.isFocusHighlighting)
    override val accentColor: StateFlow<Long?> =
        settings.mapLatest { it.base.accentColor }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), settings.value.base.accentColor)
    override val fontSize: StateFlow<Float?> =
        settings.mapLatest { it.base.fontSize }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), settings.value.base.fontSize)
    override val displaySize: StateFlow<Float?> =
        settings.mapLatest { it.base.displaySize }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), settings.value.base.displaySize)
    override val applySystemSizes: StateFlow<Boolean> =
        settings.mapLatest { it.base.applySystemSizes }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), settings.value.base.applySystemSizes)

    private val backCallback = BackCallback {
        back()
    }

    init {
        registerBackCallback(backCallback)
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

    override fun toggleFocusHighlighting() {
        coroutineScope.launch {
            settings.update<MatrixMessengerSettingsBase> {
                it.copy(isFocusHighlighting = !it.isFocusHighlighting)
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

    override fun setFontSize(fontSize: Float?) {
        coroutineScope.launch {
            settings.update<MatrixMessengerSettingsBase> {
                it.copy(fontSize = fontSize)
            }
        }
    }

    override fun setDisplaySize(controlsSize: Float?) {
        coroutineScope.launch {
            settings.update<MatrixMessengerSettingsBase> {
                it.copy(displaySize = controlsSize)
            }
        }
    }

    override fun toggleApplySystemSizes() {
        coroutineScope.launch {
            settings.update<MatrixMessengerSettingsBase> {
                it.copy(applySystemSizes = !it.applySystemSizes)
            }
        }
    }

    override fun back() {
        onCloseAppearanceSettings()
    }
}
