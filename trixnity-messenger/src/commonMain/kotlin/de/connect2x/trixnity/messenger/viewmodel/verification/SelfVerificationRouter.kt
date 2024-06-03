package de.connect2x.trixnity.messenger.viewmodel.verification

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.backStack
import com.arkivanov.decompose.router.stack.childStack
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
) {
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
                                selfVerificationConfig.userId
                            ),
                            onCloseSelfVerificationView = ::closeSelfVerificationView,
                            onCloseSelfVerification = { closeSelfVerification(selfVerificationConfig.userId) },
                        )
                )
            }

            is Config.RedoSelfVerification -> Wrapper.RedoSelfVerification(
                viewModelContext.get<RedoSelfVerificationViewModelFactory>()
                    .create(
                        viewModelContext = viewModelContext.childContext(
                            componentContext,
                            selfVerificationConfig.userId
                        ),
                        onStartSelfVerification = { showSelfVerification(selfVerificationConfig.userId) },
                        onClose = { closeSelfVerification(selfVerificationConfig.userId) },
                    )
            )

            is Config.Bootstrap -> {
                Wrapper.Bootstrap(
                    viewModelContext.get<BootstrapViewModelFactory>().create(
                        viewModelContext = viewModelContext.childContext(
                            componentContext,
                            selfVerificationConfig.userId
                        ),
                        onClose = ::closeBootstrap,
                    )
                )
            }
        }

    fun redoSelfVerification(userId: UserId) {
        navigation.launchPush(viewModelContext.coroutineScope, Config.RedoSelfVerification(userId))
    }

    /** @see startSelfVerificationsQueue() **/
    fun showSelfVerification(userId: UserId) {
        log.debug { "add account to self verification queue: $userId" }
        // do sequentially (for different accounts), so here just fill the list
        selfVerifications.value += userId
    }

    private fun closeSelfVerificationView() {
        viewModelContext.coroutineScope.launch {
            navigation.replaceCurrentSuspending(Config.None)
        }
    }

    fun closeSelfVerification(userId: UserId) {
        log.debug { "remove account from self verification queue: $userId}" }
        selfVerifications.value -= userId
    }

    suspend fun showBootstrap(userId: UserId) {
        // it can happen that the bootstrap is triggered twice (initial sync, then regular sync; to avoid any
        // complications, only allow one bootstrap to be shown at the time
        if (bootstrapStarted.value.not()) {
            log.debug { "show bootstrap view" }
            bootstrapStarted.value = true
            navigation.pushSuspending(Config.Bootstrap(userId))
        }
    }

    private fun closeBootstrap() = viewModelContext.coroutineScope.launch {
        log.debug { "close bootstrap view" }
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
                log.error { "Queue is empty: $currentSelfVerifications -- ${stack.backStack}" }
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

        class Bootstrap(val viewModel: BootstrapViewModel) : Wrapper()
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
        data class Bootstrap(val userId: UserId) : Config()
    }
}
