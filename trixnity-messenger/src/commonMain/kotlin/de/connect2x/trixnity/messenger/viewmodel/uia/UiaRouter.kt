package de.connect2x.trixnity.messenger.viewmodel.uia

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.clientserverapi.client.UIA
import de.connect2x.trixnity.clientserverapi.model.uia.AuthenticationType
import de.connect2x.trixnity.core.MatrixServerException
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.util.replaceAllSuspending
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.get

class UiaRouter(private val viewModelContext: ViewModelContext) {
    companion object {
        private val log: Logger = Logger("de.connect2x.trixnity.messenger.viewmodel.uia.UiaRouter")
    }

    private val i18n = viewModelContext.get<I18n>()
    private val navigation = StackNavigation<Config>()
    val stack =
        viewModelContext.childStack(
            source = navigation,
            initialConfiguration = Config.None,
            serializer = null,
            handleBackButton = false,
            childFactory = ::createChild,
            key = "UiaRouter-${Uuid.random()}",
        )

    private fun createChild(config: Config, componentContext: ComponentContext): Wrapper =
        when (config) {
            is Config.None -> Wrapper.None

            is Config.UiaActionConfirmation ->
                Wrapper.UiaActionConfirmation(
                    viewModelContext
                        .get<UiaActionConfirmationViewModelFactory>()
                        .create(
                            viewModelContext = viewModelContext.childContext("UiaActionConfirmation", componentContext),
                            message = config.message,
                            action = config.action,
                            onNext = ::next,
                            onCancel = ::cancel,
                            onError = ::error,
                        )
                )

            is Config.UiaStepDummy ->
                Wrapper.UiaStepDummy(
                    viewModelContext
                        .get<UiaStepDummyViewModelFactory>()
                        .create(
                            viewModelContext = viewModelContext.childContext("UiaStepDummy", componentContext),
                            uiaStep = config.uiaStep,
                            onNext = ::next,
                            onCancel = ::cancel,
                            onError = ::error,
                        )
                )

            is Config.UiaStepPassword ->
                Wrapper.UiaStepPassword(
                    viewModelContext
                        .get<UiaStepPasswordViewModelFactory>()
                        .create(
                            viewModelContext = viewModelContext.childContext("UiaStepPassword", componentContext),
                            uiaStep = config.uiaStep,
                            onNext = ::next,
                            onCancel = ::cancel,
                            onError = ::error,
                        )
                )

            is Config.UiaStepRegistrationToken ->
                Wrapper.UiaStepRegistrationToken(
                    viewModelContext
                        .get<UiaStepRegistrationTokenViewModelFactory>()
                        .create(
                            viewModelContext =
                                viewModelContext.childContext("UiaStepRegistrationToken", componentContext),
                            uiaStep = config.uiaStep,
                            onNext = ::next,
                            onCancel = ::cancel,
                            onError = ::error,
                        )
                )

            is Config.UiaStepEmailIdentity ->
                Wrapper.UiaStepEmailIdentity(
                    viewModelContext
                        .get<UiaStepEmailIdentityViewModelFactory>()
                        .create(
                            viewModelContext = viewModelContext.childContext("UiaStepEmailIdentity", componentContext),
                            onCancel = ::cancel,
                        )
                )

            is Config.UiaStepMsisdn ->
                Wrapper.UiaStepMsisdn(
                    viewModelContext
                        .get<UiaStepMsisdnViewModelFactory>()
                        .create(
                            viewModelContext = viewModelContext.childContext("UiaStepMsisdn", componentContext),
                            onCancel = ::cancel,
                        )
                )

            is Config.UiaStepFallback ->
                Wrapper.UiaStepFallback(
                    viewModelContext
                        .get<UiaStepFallbackViewModelFactory>()
                        .create(
                            viewModelContext = viewModelContext.childContext("UiaStepFallback", componentContext),
                            uiaStep = config.uiaStep,
                            authenticationType = config.authenticationType,
                            onNext = ::next,
                            onCancel = ::cancel,
                            onError = ::error,
                        )
                )
        }

    sealed class Wrapper {
        data object None : Wrapper()

        class UiaActionConfirmation(val viewModel: UiaActionConfirmationViewModel) : Wrapper()

        class UiaStepDummy(val viewModel: UiaStepDummyViewModel) : Wrapper()

        class UiaStepPassword(val viewModel: UiaStepPasswordViewModel) : Wrapper()

        class UiaStepRegistrationToken(val viewModel: UiaStepRegistrationTokenViewModel) : Wrapper()

        class UiaStepEmailIdentity(val viewModel: UiaStepEmailIdentityViewModel) : Wrapper()

        class UiaStepMsisdn(val viewModel: UiaStepMsisdnViewModel) : Wrapper()

        class UiaStepFallback(val viewModel: UiaStepFallbackViewModel) : Wrapper()
    }

    sealed class Config {
        data object None : Config()

        data class UiaActionConfirmation(val message: String?, val action: suspend () -> Result<UIA<*>>) : Config()

        data class UiaStepDummy(val uiaStep: UIA.Step<*>) : Config()

        data class UiaStepPassword(val uiaStep: UIA.Step<*>) : Config()

        data class UiaStepRegistrationToken(val uiaStep: UIA.Step<*>) : Config()

        data class UiaStepEmailIdentity(val uiaStep: UIA.Step<*>) : Config()

        data class UiaStepMsisdn(val uiaStep: UIA.Step<*>) : Config()

        data class UiaStepFallback(val uiaStep: UIA.Step<*>, val authenticationType: AuthenticationType) : Config()
    }

    private val authFlow = viewModelContext.get<AuthorizeUia>()
    private val onResult = MutableSharedFlow<AuthorizeUiaResult<*>>()

    init {
        viewModelContext.coroutineScope.launch {
            authFlow.onRequestFlow.collectLatest { params ->
                coroutineScope {
                    val result = async(start = CoroutineStart.UNDISPATCHED) { onResult.first() }
                    navigation.replaceAllSuspending(
                        Config.UiaActionConfirmation(params.confirmationMessage, params.action)
                    )
                    params.onResult(result.await())
                    navigation.replaceAllSuspending(Config.None)
                }
            }
        }
    }

    private fun next(uia: UIA<*>) {
        viewModelContext.coroutineScope.launch {
            when (uia) {
                is UIA.Success -> {
                    log.debug { "UIA was successful" }
                    success(uia)
                }

                is UIA.Step -> {
                    val nextStepAuthenticationType = nextStepAuthenticationType(uia)
                    log.debug { "handle UIA step with next type: $nextStepAuthenticationType" }
                    if (nextStepAuthenticationType != null) {
                        val nextConfig =
                            when (nextStepAuthenticationType) {
                                AuthenticationType.Dummy -> Config.UiaStepDummy(uia)
                                AuthenticationType.Password -> Config.UiaStepPassword(uia)
                                AuthenticationType.RegistrationToken -> Config.UiaStepRegistrationToken(uia)
                                AuthenticationType.SSO -> Config.UiaStepFallback(uia, nextStepAuthenticationType)
                                AuthenticationType.EmailIdentity -> Config.UiaStepEmailIdentity(uia)
                                AuthenticationType.Msisdn -> Config.UiaStepMsisdn(uia)
                                else -> Config.UiaStepFallback(uia, nextStepAuthenticationType)
                            }
                        navigation.replaceAllSuspending(nextConfig)
                    } else {
                        log.error { "could not continue UIA, because no next step found" }
                        unexpectedError(i18n.uiaGenericError("INTERNAL_NO_STEP"))
                    }
                }

                is UIA.Error -> {
                    log.error {
                        "could not continue UIA, because UIA.Error found -> this should be handled by view models"
                    }
                    unexpectedError(i18n.uiaGenericError("INTERNAL_UNHANDLED_ERROR"))
                }
            }
        }
    }

    private fun nextStepAuthenticationType(uia: UIA<*>): AuthenticationType? {
        val nextFlows: List<AuthenticationType>
        val completed: List<AuthenticationType>
        when (uia) {
            is UIA.Step<*> -> {
                nextFlows = uia.state.flows.firstOrNull()?.stages.orEmpty()
                completed = uia.state.completed
            }

            is UIA.Error<*> -> {
                nextFlows = uia.state.flows.firstOrNull()?.stages.orEmpty()
                completed = uia.state.completed
            }

            else -> return null
        }
        return (nextFlows - completed.toSet()).firstOrNull()
    }

    private fun cancel() {
        viewModelContext.coroutineScope.launch {
            onResult.emit(AuthorizeUiaResult.CancelledByUser<Nothing>(i18n.uiaCancelledByUser()))
        }
    }

    private fun error(exception: MatrixServerException) {
        viewModelContext.coroutineScope.launch { onResult.emit(AuthorizeUiaResult.Error<Nothing>(exception)) }
    }

    private suspend fun unexpectedError(message: String) {
        onResult.emit(AuthorizeUiaResult.UnexpectedError<Nothing>(message))
    }

    private suspend fun success(uia: UIA.Success<*>) {
        onResult.emit(AuthorizeUiaResult.Success(uia))
    }
}
