package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.ThemeMode
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import korlibs.io.async.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
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
    val themeMode: StateFlow<ThemeMode?>
    val isHighContrast: StateFlow<Boolean?>
    val accentColor: StateFlow<Long?>

    fun setThemeMode(themeMode: ThemeMode)
    fun toggleHighContrast()
    fun setAccentColor(accentColor: Long?)
    fun back()
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppearanceSettingsViewModelImpl(
    private val viewModelContext: ViewModelContext,
    private val onCloseAppearanceSettings: () -> Unit
) : ViewModelContext by viewModelContext, AppearanceSettingsViewModel {
    private val settings = get<MatrixMessengerSettingsHolder>()

    override val themeMode: StateFlow<ThemeMode?> =
        settings.mapLatest { it.themeMode }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val isHighContrast: StateFlow<Boolean?> =
        settings.mapLatest { it.isHighContrast }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
    override val accentColor: StateFlow<Long?> =
        settings.mapLatest { it.accentColor }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    private val backCallback = BackCallback {
        back()
    }

    init {
        backHandler.register(backCallback)
    }

    override fun setThemeMode(themeMode: ThemeMode) {
        coroutineScope.launch {
            settings.update {
                it.copy(themeMode = themeMode)
            }
        }
    }

    override fun toggleHighContrast() {
        coroutineScope.launch {
            settings.update {
                it.copy(isHighContrast = !it.isHighContrast)
            }
        }
    }

    override fun setAccentColor(accentColor: Long?) {
        coroutineScope.launch {
            settings.update {
                it.copy(accentColor = accentColor)
            }
        }
    }

    override fun back() {
        onCloseAppearanceSettings()
    }
}
