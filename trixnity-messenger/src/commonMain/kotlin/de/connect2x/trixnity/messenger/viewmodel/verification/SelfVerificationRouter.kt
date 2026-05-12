package de.connect2x.trixnity.messenger.viewmodel.verification

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.backStack
import com.arkivanov.decompose.router.stack.childStack
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.util.launchPop
import de.connect2x.trixnity.messenger.util.launchPush
import de.connect2x.trixnity.messenger.util.popWhileSuspending
import de.connect2x.trixnity.messenger.util.replaceCurrentSuspending
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.matrixClients
import de.connect2x.trixnity.messenger.viewmodel.util.scopedCollectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import de.connect2x.trixnity.client.verification
import de.connect2x.trixnity.client.verification.VerificationService
import de.connect2x.trixnity.core.model.UserId
import org.koin.core.component.get

class SelfVerificationRouter(
    private val viewModelContext: ViewModelContext,
    private val onCloseSelfVerification: (userId: UserId, completedVerification: Boolean) -> Unit
) : ViewModelContext by viewModelContext {
    private val selfVerifications =
        MutableStateFlow(setOf<UserId>()) // in case of multiple self verifications, we need to do one after another
    private val crossSigningBootstraps = MutableStateFlow(setOf<UserId>())

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
                                "SelfVerification",
                                componentContext,
                                selfVerificationConfig.userId,
                            ),
                            onCloseSelfVerification = { completedVerification ->
                                closeSelfVerification(selfVerificationConfig.userId)
                                onCloseSelfVerification(selfVerificationConfig.userId, completedVerification)
                            },
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
                            "RedoSelfVerification",
                            componentContext,
                            selfVerificationConfig.userId,
                        ),
                        onStartSelfVerification = { showSelfVerification(selfVerificationConfig.userId, true) },
                        onClose = ::continueWithoutVerification,
                    )
            )

            is Config.CrossSigningBootstrap -> {
                Wrapper.CrossSigningBootstrap(
                    viewModelContext.get<CrossSigningBootstrapViewModelFactory>().create(
                        viewModelContext = viewModelContext.childContext(
                            "CrossSigningBootstrap",
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
    fun showSelfVerification(userId: UserId, isFromSetup: Boolean = false) {
        log.debug { "add account to self verification queue: $userId" }
        if (messengerSettings.value.base.accounts.any { !it.value.base.accountSetupFinished } && !isFromSetup) {
            log.debug { "At least one account isn't set up with the wizard, not showing self verification for $userId" }
        } else if (!crossSigningBootstraps.value.isEmpty()) {
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

    private fun listenForCrossSigningNotEnabled() {
        coroutineScope.launch {
            matrixClients.scopedCollectLatest { namedMatrixClients ->
                namedMatrixClients.forEach { (userId, client) ->
                    launch {
                        log.debug { "launch listen for self verification methods (account $userId)" }
                        client.verification.getSelfVerificationMethods().distinctUntilChanged().collect {
                            if (it is VerificationService.SelfVerificationMethods.NoCrossSigningEnabled) {
                                log.debug { "found Cross Signing to be disabled for account $userId, starting bootstrap" }
                                showCrossSigningBootstrap(userId)
                            }
                        }
                    }
                }
            }
        }
    }


    fun showCrossSigningBootstrap(userId: UserId) {
        // it can happen that the bootstrap is triggered twice (initial sync, then regular sync; to avoid any
        // complications, only allow one bootstrap to be shown at the time
        crossSigningBootstraps.value += userId
        log.debug { "added $userId to crossSigningBootstrap queue" }
    }

    private fun closeCrossSigningBootstrap(userId: UserId) = viewModelContext.coroutineScope.launch {
        log.debug { "close cross signing bootstrap view" }
        onCloseSelfVerification(userId, true)
        if (stack.backStack.any { it.configuration is Config.None }) {
            navigation.popWhileSuspending(predicate = { it !is Config.None }, onComplete = {
                log.debug { "close bootstrap completed: $it" }
                crossSigningBootstraps.value -= userId
            })
        } else {
            navigation.replaceCurrentSuspending(Config.None) {
                log.debug { "close bootstrap completed" }
                crossSigningBootstraps.value -= userId
            }
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
    private fun startSelfVerificationsQueue() = viewModelContext.coroutineScope.launch {
        log.debug { "Starting self verification queue" }
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

    private fun startCrossSigningQueue() {
        log.debug { "Starting cross signing bootstrap queue" }
        coroutineScope.launch {
            crossSigningBootstraps.collect { currentBootstraps ->
                log.trace { "current bootstraps: $currentBootstraps" }
                val nextBootstrap = currentBootstraps.firstOrNull()
                if (nextBootstrap != null) {
                    navigation.replaceCurrentSuspending(
                        Config.CrossSigningBootstrap(nextBootstrap)
                    )
                }
            }
        }
    }

    init {
        listenForCrossSigningNotEnabled()
        startSelfVerificationsQueue()
        startCrossSigningQueue()
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
