package de.connect2x.trixnity.messenger.viewmodel.uia

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import de.connect2x.trixnity.clientserverapi.client.UIA
import de.connect2x.trixnity.clientserverapi.model.uia.AuthenticationType
import de.connect2x.trixnity.core.MatrixServerException
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.internal.uia.UIAController
import de.connect2x.trixnity.messenger.internal.uia.UIADestination
import de.connect2x.trixnity.messenger.internal.uia.UIALogic
import de.connect2x.trixnity.messenger.internal.uia.UIANavigator
import de.connect2x.trixnity.messenger.internal.uia.UIASessionHolder
import de.connect2x.trixnity.messenger.internal.workers.UIAWorker
import de.connect2x.trixnity.messenger.util.replaceAllSuspending
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import kotlin.uuid.Uuid
import kotlinx.coroutines.launch
import org.koin.core.component.get

class UiaRouter(private val viewModelContext: ViewModelContext) {
    private val navigation = StackNavigation<Config>()

    private val uiaProvider = UiaProvider(navigation = navigation, viewModelContext = viewModelContext)

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

    private fun next(uia: UIA<*>) {
        viewModelContext.coroutineScope.launch { uiaProvider.uiaController.next(uia) }
    }

    private fun cancel() {
        viewModelContext.coroutineScope.launch { uiaProvider.uiaController.cancel() }
    }

    private fun error(exception: MatrixServerException) {
        viewModelContext.coroutineScope.launch { uiaProvider.uiaController.error(exception) }
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

    companion object
}

private class UiaProvider(navigation: StackNavigation<UiaRouter.Config>, viewModelContext: ViewModelContext) {
    private val uiaNavigator =
        object : UIANavigator {
            override suspend fun navigate(destination: UIADestination) {
                navigation.replaceAllSuspending(mapDestination(destination))
            }
        }
    private val uiaSessions = UIASessionHolder()
    private val uiaWorker =
        UIAWorker(authorizeUia = viewModelContext.get<AuthorizeUia>(), sessions = uiaSessions, navigator = uiaNavigator)

    val uiaController =
        UIAController(
            i18n = viewModelContext.get<I18n>(),
            logic = UIALogic(),
            sessions = uiaSessions,
            navigator = uiaNavigator,
        )

    init {
        viewModelContext.coroutineScope.launch { uiaWorker.doWork() }
    }

    private fun mapDestination(destination: UIADestination): UiaRouter.Config {
        return when (destination) {
            UIADestination.None -> UiaRouter.Config.None
            is UIADestination.Confirm ->
                UiaRouter.Config.UiaActionConfirmation(destination.confirmationMessage, destination.action)

            is UIADestination.Dummy -> UiaRouter.Config.UiaStepDummy(destination.step)
            is UIADestination.Password -> UiaRouter.Config.UiaStepPassword(destination.step)
            is UIADestination.RegistrationToken -> UiaRouter.Config.UiaStepRegistrationToken(destination.step)
            is UIADestination.EmailIdentity -> UiaRouter.Config.UiaStepEmailIdentity(destination.step)
            is UIADestination.Msisdn -> UiaRouter.Config.UiaStepMsisdn(destination.step)
            is UIADestination.Fallback ->
                UiaRouter.Config.UiaStepFallback(destination.step, destination.authenticationType)
        }
    }
}
