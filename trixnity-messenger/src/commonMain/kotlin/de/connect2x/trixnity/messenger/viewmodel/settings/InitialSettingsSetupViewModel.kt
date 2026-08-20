package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.settings.NotificationSettingsSingleAccountViewModel.NotificationProviderViewModel
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.get

interface InitialSettingsSetupViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        notificationsSettingsSingleAccountViewModel: NotificationSettingsSingleAccountViewModel,
    ): InitialSettingsSetupViewModel =
        InitialSettingsSetupViewModelImpl(viewModelContext, notificationsSettingsSingleAccountViewModel)

    companion object : InitialSettingsSetupViewModelFactory
}

/** During setup, the privacy settings are displayed in a condensed form. */
interface InitialSettingsSetupViewModel {
    /** For which account the privacy settings are displayed. */
    val account: UserId

    /** If true, the user is not visible to other users (online presence, typing, etc.). */
    val strictPrivacyEnabled: StateFlow<Boolean>

    /** If true, the user will receive notifications for new messages. */
    val notificationsEnabled: StateFlow<Boolean>

    /** A list containing all notification providers (FCM, APNS, UnifiedPush, etc.) */
    val availableProviders: List<NotificationProviderViewModel>

    /** If there are multiple notification providers, this represents the user selected provider. */
    val selectedProvider: StateFlow<NotificationProviderViewModel?>

    /** Only needed for Android notification handlers. */
    val notificationHandlerId: String

    /**
     * Whether the user needs to grant permissions for notifications to work. Can be used to show a notice to the user.
     */
    val notificationPermissionsNecessary: StateFlow<Boolean>

    /** Will set the privacy to the other mode (strict or liberal). */
    fun toggleStrictPrivacy()

    /** Will enable or disable notifications based on the current setting. */
    fun toggleNotifications()

    /** In case multiple notification providers are active, the user can choose one via this method. */
    fun selectProvider(id: String)
}

class InitialSettingsSetupViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    private val notificationsSettingsSingleAccountViewModel: NotificationSettingsSingleAccountViewModel,
) : InitialSettingsSetupViewModel, MatrixClientViewModelContext by viewModelContext {

    private val messengerSettings = get<MatrixMessengerSettingsHolder>()
    private val accountSettings = messengerSettings[userId]

    override val account: UserId = userId

    override val strictPrivacyEnabled: StateFlow<Boolean> =
        accountSettings
            .filterNotNull()
            .map { settings ->
                (settings.base.presenceIsPublic || settings.base.typingIsPublic || settings.base.readMarkerIsPublic)
                    .not()
            }
            .stateIn(coroutineScope, Eagerly, false)

    override val notificationsEnabled: StateFlow<Boolean> =
        notificationsSettingsSingleAccountViewModel.enabledForThisDevice
    override val availableProviders: List<NotificationProviderViewModel> =
        notificationsSettingsSingleAccountViewModel.availableProviders
    override val selectedProvider: StateFlow<NotificationProviderViewModel?> =
        notificationsSettingsSingleAccountViewModel.selectedProvider
    override val notificationHandlerId: String = notificationsSettingsSingleAccountViewModel.notificationHandlerId
    override val notificationPermissionsNecessary: StateFlow<Boolean> =
        notificationsSettingsSingleAccountViewModel.notificationPermissionsNecessary

    override fun toggleStrictPrivacy() {
        coroutineScope.launch {
            messengerSettings.update<MatrixMessengerAccountSettingsBase>(account) {
                val newValue = strictPrivacyEnabled.value
                it.copy(presenceIsPublic = newValue, typingIsPublic = newValue, readMarkerIsPublic = newValue)
            }
        }
    }

    override fun toggleNotifications() {
        notificationsSettingsSingleAccountViewModel.toggleEnabledForThisDevice()
    }

    override fun selectProvider(id: String) {
        notificationsSettingsSingleAccountViewModel.selectProvider(id)
    }
}
