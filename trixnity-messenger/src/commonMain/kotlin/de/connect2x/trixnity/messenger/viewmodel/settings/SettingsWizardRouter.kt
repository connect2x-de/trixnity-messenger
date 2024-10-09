package de.connect2x.trixnity.messenger.viewmodel.settings

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import de.connect2x.trixnity.messenger.MatrixMessengerAccountSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.util.launchPush
import de.connect2x.trixnity.messenger.util.popWhileSuspending
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import de.connect2x.trixnity.messenger.viewmodel.settings.SettingsWizardRouter.WizardSteps.WizardConfirm
import de.connect2x.trixnity.messenger.viewmodel.settings.SettingsWizardRouter.WizardSteps.WizardExplanation
import de.connect2x.trixnity.messenger.viewmodel.settings.SettingsWizardRouter.WizardSteps.WizardNotificationSettings
import de.connect2x.trixnity.messenger.viewmodel.settings.SettingsWizardRouter.WizardSteps.WizardPrivacySettings
import de.connect2x.trixnity.messenger.viewmodel.settings.SettingsWizardRouter.WizardSteps.WizardVerification
import de.connect2x.trixnity.messenger.viewmodel.settings.SettingsWizardRouter.Wrapper
import de.connect2x.trixnity.messenger.viewmodel.util.isVerified
import de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationRouter
import de.connect2x.trixnity.messenger.viewmodel.verification.SelfVerificationViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationRouter
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.folivo.trixnity.client.key
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get
import kotlin.reflect.KClass

private val log = KotlinLogging.logger { }

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

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsWizardRouter(
    private val viewModelContext: ViewModelContext,
    val verificationRouter: VerificationRouter,
    val selfVerificationRouter: SelfVerificationRouter
) : ViewModelContext by viewModelContext {


    private val stepClasses = get<SettingsWizardSteps>().steps

    val navigation = StackNavigation<Config>()
    val stack = viewModelContext.childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.None,
        key = "SettingsWizardRouter,",
        childFactory = ::createChild
    )


    private val activeAccount = MutableStateFlow<UserId?>(null)

    private val verificationViewModel = MutableStateFlow<VerificationViewModel?>(null)

    init {
        log.debug { "Initializing Wizard" }
        coroutineScope.launch {
            get<MatrixMessengerSettingsHolder>().collect {
                activeAccount.value = it.base.selectedAccount
            }
        }
    }


    val selfVerificationViewModel = MutableStateFlow<SelfVerificationViewModel?>(null)

    private val isVerified =
        combine(activeAccount, matrixClients) { account, clients -> clients[account] }.map {
            it?.key?.getTrustLevel(it.userId, it.deviceId)
                ?.map { it.isVerified }
        }.filterNotNull().flatMapLatest { it }
            .stateIn(coroutineScope, SharingStarted.WhileSubscribed(), false)

    fun startVerification() {
        activeAccount.value?.let { selfVerificationRouter.showSelfVerification(it) }
        coroutineScope.launch {
            selfVerificationRouter.stack.subscribe {
                if (it.active.instance is SelfVerificationRouter.Wrapper.View) selfVerificationViewModel.value =
                    (it.active.instance as SelfVerificationRouter.Wrapper.View).viewModel
            }
        }
    }


    fun startCrossVerification() {
        coroutineScope.launch {
            activeAccount.value?.let { verificationRouter.startDeviceVerification(it) }
            val active = verificationRouter.stack.value.active
            if (active.instance is VerificationRouter.Wrapper.Verification) {
                verificationViewModel.value = (active.instance as VerificationRouter.Wrapper.Verification).viewModel
            }
        }
    }

    private fun createChild(config: Config, componentContext: ComponentContext): Wrapper = when (config) {
        is Config.None -> {
            Wrapper.None
        }

        is Config.ShowWizard -> {
            log.debug { "Building Settings Wizard" }
            Wrapper.ShowWizard(mutableListOf<Wrapper>().apply {
                stepClasses.forEach {
                    when (it) {
                        WizardExplanation::class -> this.add(WizardExplanation(activeAccount, ::onWizardClose))
                        WizardPrivacySettings::class -> {
                            val account = activeAccount.value
                            if (account != null) {
                                this.add(WizardPrivacySettings(viewModelContext.get<PrivacySettingsAllAccountsViewModelFactory>()
                                    .create(viewModelContext.childContext(
                                        componentContext, account,
                                    ), {}, {}).privacySettings.transformLatest { value ->
                                        val element = value.find { it.account == account }
                                        if (element != null) emit(
                                            element
                                        )
                                    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(), null)
                                )
                                )
                            } else log.error { "Can't create Privacy-step for settings Wizard because user is null" }
                        }

                        WizardNotificationSettings::class -> {
                            val account = activeAccount.value
                            if (account != null) {
                                this.add(
                                    WizardNotificationSettings(
                                        viewModelContext.get<NotificationSettingsAllAccountsViewModelFactory>().create(
                                            viewModelContext.childContext(
                                                componentContext, account
                                            ),
                                        ) {}.notificationSettings.transformLatest { value ->
                                            val element = value.find { it.account == account }
                                            if (element != null) emit(
                                                element
                                            )
                                        }.stateIn(
                                            coroutineScope, SharingStarted.WhileSubscribed(), null
                                        )
                                    )
                                )
                            } else log.error { "Can't create Notification-step for settings Wizard because user is null" }
                        }

                        WizardVerification::class -> {
                            this.add(
                                WizardVerification(
                                    isVerified,
                                    selfVerificationViewModel,
                                    verificationViewModel,
                                    ::startCrossVerification,
                                    ::startVerification
                                )
                            )
                        }

                        WizardConfirm::class -> this.add(WizardConfirm(::onWizardClose))
                        else -> this.add(get<AdditionalSettingsWizardWrapper>().create(it))
                    }
                }
            })

        }
    }

    private val settings = get<MatrixMessengerSettingsHolder>()

    fun onWizardClose() {
        log.debug { "Closing Settings Wizard for ${activeAccount.value}" }
        coroutineScope.launch {
            activeAccount.value?.let {
                settings.update<MatrixMessengerAccountSettingsBase>(it) {
                    it.copy(showAccountWizard = false)
                }
            }
            navigation.popWhileSuspending {
                it !is Config.None
            }
        }
    }

    fun startWizard() {
        log.debug { "Starting Wizard" }
        navigation.launchPush(coroutineScope, Config.ShowWizard)
    }

    sealed class WizardSteps {
        data class WizardNotificationSettings(val viewModel: StateFlow<NotificationSettingsSingleAccountViewModel?>) :
            Wrapper()

        data class WizardPrivacySettings(val viewModel: StateFlow<PrivacySettingsSingleAccountViewModel?>) : Wrapper()
        data class WizardExplanation(val userId: StateFlow<UserId?>, val onWizardClose: () -> Unit) : Wrapper()
        data class WizardVerification(
            val isVerified: StateFlow<Boolean>,
            val selfVerificationViewModel: StateFlow<SelfVerificationViewModel?>,
            val verificationViewModel: StateFlow<VerificationViewModel?>,
            val startCrossSigning: () -> Unit,
            val startVerification: () -> Unit
        ) : Wrapper()

        data class WizardConfirm(val onWizardClose: () -> Unit) : Wrapper()
    }

    sealed class Wrapper {
        data class ShowWizard(val steps: List<Wrapper>) : Wrapper()
        data object None : Wrapper()
    }

    @Serializable
    sealed class Config {
        @Serializable
        data object ShowWizard : Config()

        @Serializable
        data object None : Config()
    }
}

