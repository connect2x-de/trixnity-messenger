package de.connect2x.trixnity.messenger.viewmodel.verification

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.backStack
import com.arkivanov.decompose.router.stack.childStack
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.util.launchPop
import de.connect2x.trixnity.messenger.util.launchPush
import de.connect2x.trixnity.messenger.util.popSuspending
import de.connect2x.trixnity.messenger.util.popWhileSuspending
import de.connect2x.trixnity.messenger.util.pushSuspending
import de.connect2x.trixnity.messenger.util.replaceCurrentSuspending
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.UserId
import org.koin.core.component.get

private val log = KotlinLogging.logger { }

class SelfVerificationRouter(
    private val viewModelContext: ViewModelContext,
) : ViewModelContext by viewModelContext {
    private val bootstrapStarted = MutableStateFlow(false)
    private val selfVerifications =
        MutableStateFlow(setOf<UserId>()) // in case of multiple self verifications, we need to do one after another

    private val navigation = StackNavigation<Config>()
    val stack = viewModelContext.childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.None,
        handleBackButton = false,
        childFactory = ::createSelfVerificationChild
    )

    private fun createSelfVerificationChild(
        selfVerificationConfig: Config,
        componentContext: ComponentContext
    ): Wrapper =
        when (selfVerificationConfig) {
            is Config.None -> Wrapper.None
            is Config.SelfVerification -> {
                Wrapper.View(
                    viewModelContext.get<SelfVerificationViewModelFactory>()
                        .create(
                            viewModelContext = viewModelContext.childContext(
                                componentContext,
                                selfVerificationConfig.userId,
                            ),
                            onCloseSelfVerification = { closeSelfVerification(selfVerificationConfig.userId) },
                            onResetRecovery = {
                                closeSelfVerification(selfVerificationConfig.userId)
                                viewModelContext.coroutineScope.launch {
                                    showCrossSigningBootstrap(
                                        selfVerificationConfig.userId
                                    )
                                }
                            }
                        )
                )
            }

            is Config.RedoSelfVerification -> Wrapper.RedoSelfVerification(
                viewModelContext.get<RedoSelfVerificationViewModelFactory>()
                    .create(
                        viewModelContext = viewModelContext.childContext(
                            componentContext,
                            selfVerificationConfig.userId,
                        ),
                        onStartSelfVerification = { showSelfVerification(selfVerificationConfig.userId) },
                        onClose = ::continueWithoutVerification,
                    )
            )

            is Config.CrossSigningBootstrap -> {
                Wrapper.CrossSigningBootstrap(
                    viewModelContext.get<CrossSigningBootstrapViewModelFactory>().create(
                        viewModelContext = viewModelContext.childContext(
                            componentContext,
                            selfVerificationConfig.userId,
                        ),
                        onClose = ::closeCrossSigningBootstrap,
                    )
                )
            }
        }

    fun redoSelfVerification(userId: UserId) {
        navigation.launchPush(viewModelContext.coroutineScope, Config.RedoSelfVerification(userId))
    }

    val messengerSettings = get<MatrixMessengerSettingsHolder>()
    val messengerConfiguration = get<MatrixMessengerConfiguration>()

    /** @see startSelfVerificationsQueue() **/
    fun showSelfVerification(userId: UserId) {
        log.debug { "add account to self verification queue: $userId" }
        if (messengerSettings.value.base.accounts.any { !it.value.base.accountSetupFinished } && messengerConfiguration.useAccountSetupWizard) {
            log.debug { "At least one account isn't set up with the wizard, not showing self verification for $userId" }
        } else if (bootstrapStarted.value) {
            log.debug { "bootstrapping has started, not showing self verification for: $userId" }
        } else {
            // do sequentially (for different accounts), so here just fill the list
            selfVerifications.value += userId
        }
    }

    private fun continueWithoutVerification() {
        navigation.launchPop(viewModelContext.coroutineScope)
    }

    fun closeSelfVerification(userId: UserId) {
        log.debug { "remove account from self verification queue: $userId" }
        selfVerifications.value -= userId
    }

    suspend fun showCrossSigningBootstrap(userId: UserId) {
        // it can happen that the bootstrap is triggered twice (initial sync, then regular sync; to avoid any
        // complications, only allow one bootstrap to be shown at the time
        if (bootstrapStarted.value.not()) { // Todo: use mutex
            log.debug { "show cross signing bootstrap view" }
            bootstrapStarted.value = true
            navigation.pushSuspending(Config.CrossSigningBootstrap(userId))
        }
    }

    private fun closeCrossSigningBootstrap() = viewModelContext.coroutineScope.launch {
        log.debug { "close cross signing bootstrap view" }
        bootstrapStarted.value = false
        navigation.popSuspending(onComplete = { log.debug { "close bootstrap completed: $it" } })
    }

    init {
        viewModelContext.coroutineScope.launch {
            startSelfVerificationsQueue()
        }
    }

    /** Continually checks for new self verifications in a queue and executes them sequentially. */
    // Changed to an unidirectional flow:
    //
    // To start a verification flow, add an accountName to selfVerifications
    // This method will pick that change up and navigate accordingly
    //
    // To close a verification flow, remove the accountName from selfVerifications
    // If there are still pending verifications, this method will navigate to that flow,
    // otherwise it'll close the self verification flow entirely.
    private suspend fun startSelfVerificationsQueue() {
        selfVerifications.collect { currentSelfVerifications ->
            log.trace { "current self verifications: $currentSelfVerifications" }
            val nextAccountToVerify = currentSelfVerifications.firstOrNull()
            if (nextAccountToVerify != null) {
                navigation.replaceCurrentSuspending(
                    Config.SelfVerification(nextAccountToVerify)
                )
            } else {
                // Queue is empty, close all verifications
                if (stack.backStack.any { it.configuration is Config.None }) {
                    navigation.popWhileSuspending { it !is Config.None }
                } else {
                    navigation.replaceCurrentSuspending(Config.None)
                }
            }
        }
    }

    sealed class Wrapper {
        data object None : Wrapper()
        class View(val viewModel: SelfVerificationViewModel) : Wrapper()

        class RedoSelfVerification(val viewModel: RedoSelfVerificationViewModel) : Wrapper()

        class CrossSigningBootstrap(val viewModel: CrossSigningBootstrapViewModel) : Wrapper()
    }

    @Serializable
    sealed class Config {
        @Serializable
        data object None : Config()

        @Serializable
        data class SelfVerification(val userId: UserId) : Config()

        @Serializable
        data class RedoSelfVerification(val userId: UserId) : Config()

        @Serializable
        data class CrossSigningBootstrap(val userId: UserId) : Config()
    }
}
