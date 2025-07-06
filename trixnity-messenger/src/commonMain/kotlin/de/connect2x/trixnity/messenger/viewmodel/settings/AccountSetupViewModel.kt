package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import kotlinx.coroutines.flow.MutableStateFlow
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get


interface AccountSetupViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        onWizardClose: (userId: UserId) -> Unit,
        onStartVerification: (UserId, Boolean) -> Unit,
    ): AccountSetupViewModel {
        return AccountSetupViewModelImpl(
            viewModelContext,
            onWizardClose,
            onStartVerification,
        )
    }

    companion object : AccountSetupViewModelFactory
}

interface AccountSetupViewModel {
    fun closeAccountSetup()
    fun startVerification()
    fun changeVerificationCompleteStatus(newVerificationCompleteStatus: Boolean)

    /**
     * Marks whether the current verification was completed/skipped or cancelled.
     * A value of null means, that no verification is in process
     */
    val completedVerification: MutableStateFlow<Boolean?>
    val userId: UserId
    val appearanceSettingsViewModel: AppearanceSettingsViewModel
    val privacySettingsViewModel: PrivacySettingsSingleAccountViewModel
    val notificationSettingsViewModel: NotificationSettingsSingleAccountViewModel
}

class AccountSetupViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    val onWizardClose: (UserId) -> Unit,
    val onStartVerification: (UserId, Boolean) -> Unit,
) :
    ViewModelContext by viewModelContext, AccountSetupViewModel {
    override val userId = viewModelContext.userId
    override val appearanceSettingsViewModel: AppearanceSettingsViewModel by lazy {
        get<AppearanceSettingsViewModelFactory>().create(viewModelContext) {}
    }

    override val completedVerification: MutableStateFlow<Boolean?> = MutableStateFlow(null)

    override val privacySettingsViewModel by lazy {
        get<PrivacySettingsSingleAccountViewModelFactory>().create(viewModelContext) {}
    }
    override val notificationSettingsViewModel by lazy {
        get<NotificationSettingsSingleAccountViewModelFactory>().create(viewModelContext)
    }

    private val verificationInProgress = MutableStateFlow(false)

    override fun startVerification() {
        if (!verificationInProgress.value) {
            onStartVerification(userId, true)
            verificationInProgress.value = true
            completedVerification.value = null
        }
    }

    override fun closeAccountSetup() {
        this.onWizardClose(userId)
    }

    override fun changeVerificationCompleteStatus(newVerificationCompleteStatus: Boolean) {
        completedVerification.value = newVerificationCompleteStatus
        verificationInProgress.value = false
    }

}
