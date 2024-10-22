package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import de.connect2x.trixnity.messenger.viewmodel.util.isVerified
import de.connect2x.trixnity.messenger.viewmodel.verification.BootstrapViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.BootstrapViewModelFactory
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

interface AccountBootstrappingViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        onWizardClose: (userId: UserId) -> Unit,
        onStartVerificationBootstrap: (userId: UserId) -> Unit,
        onStartCrossDeviceVerification: (userId: UserId) -> Unit
    ): AccountBootstrappingViewModel {
        return AccountBootstrappingViewModelImpl(
            viewModelContext, onWizardClose, onStartVerificationBootstrap, onStartCrossDeviceVerification
        )
    }

    companion object : AccountBootstrappingViewModelFactory
}

interface AccountBootstrappingViewModel {
    fun closeWizard()
    fun startBootstrap()
    fun startCrossDeviceVerification()
    val userId: UserId
    val privacySettingsViewModel: PrivacySettingsSingleAccountViewModel
    val notificationSettingsViewModel: NotificationSettingsSingleAccountViewModel
    val verificationViewModel: VerificationViewModel
    val selfVerificationViewModel: SelfVerificationViewModel
    val bootstrapViewModel: BootstrapViewModel
    val isVerified: StateFlow<Boolean?>

}

class AccountBootstrappingViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    val onWizardClose: (UserId) -> Unit,
    val onStartVerificationBootstrap: (UserId) -> Unit,
    val onStartCrossDeviceVerification: (UserId) -> Unit
) :
    ViewModelContext by viewModelContext, AccountBootstrappingViewModel {
    override val userId = viewModelContext.userId

    override val privacySettingsViewModel by lazy {
        get<PrivacySettingsSingleAccountViewModelFactory>().create(viewModelContext) {}
    }
    override val notificationSettingsViewModel by lazy {
        get<NotificationSettingsSingleAccountViewModelFactory>().create(viewModelContext)
    }

    override val bootstrapViewModel by lazy {
        get<BootstrapViewModelFactory>().create(viewModelContext, {})
    }

    override fun startBootstrap() {
        log.debug { "Start Verification bootstrap from AccountBootstrapping" }
        onStartVerificationBootstrap(userId)
    }

    override fun startCrossDeviceVerification() {
        log.debug { "Start Cross Device Verification from AccountBootstrapping" }
        onStartCrossDeviceVerification(userId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val isVerified =
        matrixClients.map { it[userId] }.filterNotNull()
            .map { it.key.getTrustLevel(userId, it.deviceId).map { it.isVerified } }.flatMapLatest({ it })
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override val selfVerificationViewModel by lazy {
        get<SelfVerificationViewModelFactory>().create(viewModelContext, {}, ::startBootstrap)
    }
    override val verificationViewModel by lazy {
        get<VerificationViewModelFactory>().create(viewModelContext, {}, {}, null, null)
    }

    override fun closeWizard() {
        this.onWizardClose(userId)
    }
}
