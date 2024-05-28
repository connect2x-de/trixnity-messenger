package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.MatrixMessengerSettings
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
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
    /**
     * A flag which indicates the dark theme state in one of three ways:
     *  - `null`: Use the system default
     *  - `false`: Force using the light theme
     *  - `true`: Force using the dark theme
     */
    val isDarkTheme: StateFlow<Boolean?>

    /**
     * A flag which indicates whether to use a high contrast theme.
     */
    val isHighContrast: StateFlow<Boolean?>

    /**
     * A packed RGBA color used as an accent throughout the messenger.
     * `null` if the default color should be used.
     */
    val accentColor: StateFlow<Long?>

    /**
     * Sets the dark mode.
     * @see [isDarkTheme].
     */
    fun setIsDarkTheme(isDarkTheme: Boolean?)

    /**
     * Toggles the high contrast mode for the current theme.
     * @see [isHighContrast]
     */
    fun toggleHighContrast()

    /**
     * Sets the accent color.
     * @see [accentColor].
     */
    fun setAccentColor(color: Long?)

    /**
     * Returns to the previous view.
     */
    fun back()
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppearanceSettingsViewModelImpl(
    private val viewModelContext: ViewModelContext,
    private val onCloseAppearanceSettings: () -> Unit
) : ViewModelContext by viewModelContext, AppearanceSettingsViewModel {
    private val settings = get<MatrixMessengerSettingsHolder>()

    override val isDarkTheme: StateFlow<Boolean?> =
        settings.mapLatest { it.isDarkTheme }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
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

    override fun setIsDarkTheme(isDarkTheme: Boolean?) {
        coroutineScope.launch {
            settings.update {
                MatrixMessengerSettings(
                    secretByteArrayKey = it.secretByteArrayKey,
                    accounts = it.accounts,
                    preferredLang = it.preferredLang,
                    selectedAccount = it.selectedAccount,
                    ssoState = it.ssoState,
                    isDarkTheme = isDarkTheme,
                    isHighContrast = it.isHighContrast,
                    accentColor = it.accentColor
                )
            }
        }
    }

    override fun toggleHighContrast() {
        coroutineScope.launch {
            settings.update {
                MatrixMessengerSettings(
                    secretByteArrayKey = it.secretByteArrayKey,
                    accounts = it.accounts,
                    preferredLang = it.preferredLang,
                    selectedAccount = it.selectedAccount,
                    ssoState = it.ssoState,
                    isDarkTheme = it.isDarkTheme,
                    isHighContrast = !it.isHighContrast,
                    accentColor = it.accentColor
                )
            }
        }
    }

    override fun setAccentColor(color: Long?) {
        coroutineScope.launch {
            settings.update {
                MatrixMessengerSettings(
                    secretByteArrayKey = it.secretByteArrayKey,
                    accounts = it.accounts,
                    preferredLang = it.preferredLang,
                    selectedAccount = it.selectedAccount,
                    ssoState = it.ssoState,
                    isDarkTheme = it.isDarkTheme,
                    isHighContrast = it.isHighContrast,
                    accentColor = color
                )
            }
        }
    }

    override fun back() {
        onCloseAppearanceSettings()
    }
}
