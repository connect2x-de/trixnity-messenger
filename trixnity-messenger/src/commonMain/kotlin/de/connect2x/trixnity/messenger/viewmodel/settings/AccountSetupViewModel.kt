package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import de.connect2x.trixnity.messenger.viewmodel.util.isVerified
import de.connect2x.trixnity.messenger.viewmodel.verification.CrossSigningBootstrapViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.CrossSigningBootstrapViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModelFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.client.key
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get

private val log = KotlinLogging.logger { }

interface AccountSetupViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        onWizardClose: (userId: UserId) -> Unit,
        onStartCrossSigningBootstrap: (userId: UserId) -> Unit,
        onCloseCrossDeviceVerification: () -> Unit
    ): AccountSetupViewModel {
        return AccountSetupViewModelImpl(
            viewModelContext, onWizardClose, onStartCrossSigningBootstrap, onCloseCrossDeviceVerification
        )
    }

    companion object : AccountSetupViewModelFactory
}

interface AccountSetupViewModel {
    fun closeAccountSetup()
    fun closeCrossDeviceVerification()
    val userId: UserId
    val privacySettingsViewModel: PrivacySettingsSingleAccountViewModel
    val notificationSettingsViewModel: NotificationSettingsSingleAccountViewModel
    val verificationViewModel: VerificationViewModel
    val selfVerificationViewModel: SelfVerificationViewModel
    val isVerified: StateFlow<Boolean?>

}

class AccountSetupViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    val onWizardClose: (UserId) -> Unit,
    val onStartCrossSigningBootstrap: (UserId) -> Unit,
    val onCloseCrossDeviceVerification: () -> Unit
) :
    ViewModelContext by viewModelContext, AccountSetupViewModel {
    override val userId = viewModelContext.userId

    override val privacySettingsViewModel by lazy {
        get<PrivacySettingsSingleAccountViewModelFactory>().create(viewModelContext) {}
    }
    override val notificationSettingsViewModel by lazy {
        get<NotificationSettingsSingleAccountViewModelFactory>().create(viewModelContext)
    }

    private fun startCrossSigningBootstrap() {
        log.debug { "Start cross signing bootstrap from AccountBootstrapping" }
        onStartCrossSigningBootstrap(userId)
    }

    override fun closeCrossDeviceVerification() {
        log.debug { "Close device Verification from AccountBootstrapping" }
        onCloseCrossDeviceVerification()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val isVerified =
        matrixClients.map { it[userId] }.filterNotNull()
            .map { it.key.getTrustLevel(userId, it.deviceId).map { it.isVerified } }.flatMapLatest({ it })
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override val selfVerificationViewModel by lazy {
        get<SelfVerificationViewModelFactory>().create(viewModelContext, {}, ::startCrossSigningBootstrap)
    }
    override val verificationViewModel by lazy {
        get<VerificationViewModelFactory>().create(viewModelContext, {}, {}, null, null)
    }

    override fun closeAccountSetup() {
        this.onWizardClose(userId)
    }
}
