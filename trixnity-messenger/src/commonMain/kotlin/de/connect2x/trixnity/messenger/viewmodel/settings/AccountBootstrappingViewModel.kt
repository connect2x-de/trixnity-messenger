package de.connect2x.trixnity.messenger.viewmodel.settings

import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountBootstrappingViewModelImpl.WizardSteps.WizardConfirm
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountBootstrappingViewModelImpl.WizardSteps.WizardExplanation
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountBootstrappingViewModelImpl.WizardSteps.WizardNotificationSettings
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountBootstrappingViewModelImpl.WizardSteps.WizardPrivacySettings
import de.connect2x.trixnity.messenger.viewmodel.settings.AccountBootstrappingViewModelImpl.WizardSteps.WizardVerification
import de.connect2x.trixnity.messenger.viewmodel.settings.SettingsWizardRouter.Wrapper
import de.connect2x.trixnity.messenger.viewmodel.util.isVerified
import de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationViewModelFactory
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.key
import net.folivo.trixnity.client.verification
import net.folivo.trixnity.client.verification.SelfVerificationMethod
import net.folivo.trixnity.client.verification.VerificationService
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get
import kotlin.reflect.KClass

interface SettingsWizardSteps {
    val steps: List<KClass<out Wrapper>>
}

/**
 * The class types of the Steps that are shown in the Wizard
 */
data object SettingsWizardStepsImpl : SettingsWizardSteps {
    override val steps: List<KClass<out Wrapper>> = listOf<KClass<out Wrapper>>(
        WizardExplanation::class, WizardPrivacySettings::class,
        WizardNotificationSettings::class, WizardVerification::class, WizardConfirm::class,
    )
}

/**
 * Implement this interface in a class and change the DI to use the class
 * when searching for non-default Wizard steps to easily add new steps
 */
interface AdditionalSettingsWizardWrapper {
    fun <T : KClass<out Wrapper>> create(classType: T): Wrapper
}

class AdditionalSettingsWizardWrapperImpl() : AdditionalSettingsWizardWrapper {
    override fun <T : KClass<out Wrapper>> create(classType: T): Wrapper {
        throw IllegalArgumentException("Creating a SettingsWizard Wrapper with $classType is unsupported and requires an implementation")
    }
}

interface AccountBootstrappingViewModelFactory {
    fun create(
        viewModelContext: MatrixClientViewModelContext,
        onWizardClose: (userId: UserId) -> Unit
    ): AccountBootstrappingViewModel {
        return AccountBootstrappingViewModelImpl(viewModelContext, onWizardClose)
    }

    companion object : AccountBootstrappingViewModelFactory
}

interface AccountBootstrappingViewModel {
    val steps: List<Wrapper>
    fun closeWizard()
}

class AccountBootstrappingViewModelImpl(
    viewModelContext: MatrixClientViewModelContext,
    val onWizardClose: (UserId) -> Unit
) :
    ViewModelContext by viewModelContext, AccountBootstrappingViewModel {
    private val account = viewModelContext.userId
    private val bootstrapNecessary = MutableStateFlow(true)

    init {
        coroutineScope.launch {
            matrixClients.map { it[account] }.filterNotNull()
                .map { it.verification.getSelfVerificationMethods() == VerificationService.SelfVerificationMethods.NoCrossSigningEnabled }
                .collect {
                    bootstrapNecessary.value = it
                }

        }
    }

    val privacySettingsViewModel = get<PrivacySettingsSingleAccountViewModelFactory>().create(viewModelContext) {}
    val notificationSettingsViewModel =
        get<NotificationSettingsSingleAccountViewModelFactory>().create(viewModelContext)


    @OptIn(ExperimentalCoroutinesApi::class)
    private val isVerified =
        matrixClients.map { it[account] }.filterNotNull()
            .map { it.key.getTrustLevel(account, it.deviceId).map { it.isVerified } }.flatMapLatest({ it })
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)

    private val selfVerificationViewModel = get<SelfVerificationViewModelFactory>().create(viewModelContext) {}
    private val verificationViewModel = get<VerificationViewModelFactory>().create(viewModelContext, {}, {}, null, null)

    fun startVerification(selfVerificationMethod: SelfVerificationMethod) {
        selfVerificationViewModel.launchVerification(selfVerificationMethod)
    }

    override fun closeWizard() {
        this.onWizardClose(account)
    }


    override val steps = mutableListOf<Wrapper>().apply {
        get<SettingsWizardSteps>().steps.forEach {
            when (it) {
                WizardExplanation::class -> add(WizardExplanation(account))
                WizardConfirm::class -> add(WizardConfirm)
                WizardPrivacySettings::class -> add(WizardPrivacySettings(privacySettingsViewModel))
                WizardNotificationSettings::class -> add(WizardNotificationSettings(notificationSettingsViewModel))
                WizardVerification::class -> add(
                    WizardVerification(
                        isVerified,
                        bootstrapNecessary,
                        selfVerificationViewModel,
                        verificationViewModel,
                        ::startVerification
                    )
                )

                else -> add(get<AdditionalSettingsWizardWrapper>().create(it))
            }
        }
    }

    sealed class WizardSteps {
        data class WizardNotificationSettings(val viewModel: NotificationSettingsSingleAccountViewModel) :
            Wrapper()

        data class WizardPrivacySettings(val viewModel: PrivacySettingsSingleAccountViewModel) : Wrapper()
        data class WizardExplanation(val userId: UserId) : Wrapper()
        data class WizardVerification(
            val isVerified: StateFlow<Boolean?>,
            val needsBootstrap: StateFlow<Boolean>,
            val selfVerificationViewModel: SelfVerificationViewModel,
            val verificationViewModel: VerificationViewModel,
            val startVerification: (selfVerificationMethod: SelfVerificationMethod) -> Unit
        ) : Wrapper()

        data object WizardConfirm : Wrapper()
    }
}
