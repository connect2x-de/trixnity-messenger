package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.essenty.backhandler.BackCallback
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get


interface UserSettingsViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        onCloseUserSettings: () -> Unit,
        onShowDevicesSettings: () -> Unit,
        onShowProfile: () -> Unit,
        onShowNotificationsSettings: () -> Unit,
        onShowPrivacySettings: () -> Unit,
        onShowAppearanceSettings: () -> Unit,
        onShowSettingsWizard: (userId: UserId) -> Unit,
    ): UserSettingsViewModel {
        return UserSettingsViewModelImpl(
            viewModelContext,
            onCloseUserSettings,
            onShowDevicesSettings,
            onShowProfile,
            onShowNotificationsSettings,
            onShowPrivacySettings,
            onShowAppearanceSettings,
            onShowSettingsWizard,
        )
    }

    companion object : UserSettingsViewModelFactory
}

interface UserSettingsViewModel {
    fun closeUserSettings()
    fun showDevicesSettings()
    fun showProfile()
    fun showNotificationsSettings()
    fun showPrivacySettings()
    fun showAppearanceSettings()
    fun showSettingsWizard(userId: UserId)
}

open class UserSettingsViewModelImpl(
    viewModelContext: ViewModelContext,
    private val onCloseUserSettings: () -> Unit,
    private val onShowDevicesSettings: () -> Unit,
    private val onShowProfile: () -> Unit,
    private val onShowNotificationsSettings: () -> Unit,
    private val onShowPrivacySettings: () -> Unit,
    private val onShowAppearanceSettings: () -> Unit,
    private val onShowSettingsWizard: (userId : UserId) -> Unit,
) : ViewModelContext by viewModelContext, UserSettingsViewModel {

    private val backCallback = BackCallback {
        closeUserSettings()
    }

    init {
        backHandler.register(backCallback)
    }

    override fun closeUserSettings() {
        onCloseUserSettings()
    }

    override fun showDevicesSettings() {
        onShowDevicesSettings()
    }

    override fun showProfile() {
        onShowProfile()
    }

    override fun showNotificationsSettings() {
        onShowNotificationsSettings()
    }

    override fun showPrivacySettings() {
        onShowPrivacySettings()
    }

    override fun showAppearanceSettings() {
        onShowAppearanceSettings()
    }

    override fun showSettingsWizard(userId: UserId) {
        onShowSettingsWizard(userId)
    }
}
