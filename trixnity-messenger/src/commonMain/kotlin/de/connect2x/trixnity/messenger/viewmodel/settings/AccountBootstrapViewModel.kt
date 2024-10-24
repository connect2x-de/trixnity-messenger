package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import de.connect2x.trixnity.messenger.viewmodel.util.isVerified
import de.connect2x.trixnity.messenger.viewmodel.verification.BootstrapCrosssigningViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.BootstrapCrosssigningViewModelFactory
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
        onCloseCrossDeviceVerification: () -> Unit
    ): AccountBootstrapViewModel {
        return AccountBootstrapViewModelImpl(
            viewModelContext, onWizardClose, onStartVerificationBootstrap, onCloseCrossDeviceVerification
        )
    }

    companion object : AccountBootstrappingViewModelFactory
}

interface AccountBootstrapViewModel {
    fun closeAccountBootstrap()
    fun startVerificationBootstrap()
    fun closeCrossDeviceVerification()
    val userId: UserId
    val privacySettingsViewModel: PrivacySettingsSingleAccountViewModel
    val notificationSettingsViewModel: NotificationSettingsSingleAccountViewModel
    val verificationViewModel: VerificationViewModel
    val selfVerificationViewModel: SelfVerificationViewModel
    val bootstrapCrosssigningViewModel: BootstrapCrosssigningViewModel
    val isVerified: StateFlow<Boolean?>

}

class AccountBootstrapViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    val onWizardClose: (UserId) -> Unit,
    val onStartVerificationBootstrap: (UserId) -> Unit,
    val onCloseCrossDeviceVerification: () -> Unit
) :
    ViewModelContext by viewModelContext, AccountBootstrapViewModel {
    override val userId = viewModelContext.userId

    override val privacySettingsViewModel by lazy {
        get<PrivacySettingsSingleAccountViewModelFactory>().create(viewModelContext) {}
    }
    override val notificationSettingsViewModel by lazy {
        get<NotificationSettingsSingleAccountViewModelFactory>().create(viewModelContext)
    }

    override val bootstrapCrosssigningViewModel by lazy {
        get<BootstrapCrosssigningViewModelFactory>().create(viewModelContext, {})
    }

    override fun startVerificationBootstrap() {
        log.debug { "Start Verification bootstrap from AccountBootstrapping" }
        onStartVerificationBootstrap(userId)
    }

    override fun closeCrossDeviceVerification() {
        log.debug { "Close Device Verification from AccountBootstrapping" }
        onCloseCrossDeviceVerification()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val isVerified =
        matrixClients.map { it[userId] }.filterNotNull()
            .map { it.key.getTrustLevel(userId, it.deviceId).map { it.isVerified } }.flatMapLatest({ it })
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    override val selfVerificationViewModel by lazy {
        get<SelfVerificationViewModelFactory>().create(viewModelContext, {}, ::startVerificationBootstrap)
    }
    override val verificationViewModel by lazy {
        get<VerificationViewModelFactory>().create(viewModelContext, {}, {}, null, null)
    }

    override fun closeAccountBootstrap() {
        this.onWizardClose(userId)
    }
}
