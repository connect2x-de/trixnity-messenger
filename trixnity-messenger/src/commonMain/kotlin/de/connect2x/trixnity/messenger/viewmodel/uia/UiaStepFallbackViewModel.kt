package de.connect2x.trixnity.messenger.viewmodel.uia

import de.connect2x.trixnity.clientserverapi.client.UIA
import de.connect2x.trixnity.clientserverapi.model.uia.AuthenticationRequest
import de.connect2x.trixnity.clientserverapi.model.uia.AuthenticationType
import de.connect2x.trixnity.core.MatrixServerException
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.util.UriCaller
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepFallbackViewModelPreview.PreviewMode.AWAITING
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepFallbackViewModelPreview.PreviewMode.ERROR
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepFallbackViewModelPreview.PreviewMode.IDLE
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.launch
import org.koin.core.component.get

private val POLLING_INTERVAL = 2.seconds // not too low to prevent rate limiting

interface UiaStepFallbackViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        uiaStep: UIA.Step<*>,
        authenticationType: AuthenticationType,
        onNext: (UIA<*>) -> Unit,
        onCancel: () -> Unit,
        onError: (MatrixServerException) -> Unit,
    ): UiaStepFallbackViewModel {
        return UiaStepFallbackViewModelImpl(viewModelContext, uiaStep, authenticationType, onNext, onCancel, onError)
    }

    companion object : UiaStepFallbackViewModelFactory
}

interface UiaStepFallbackViewModel {
    val fallbackUrl: String
    val waitForResult: StateFlow<Boolean>
    val error: StateFlow<String?>
    val authenticationTypeString: String

    fun openFallbackUrl()

    /** This is automatically called when [openFallbackUrl] is used. */
    fun confirm()

    fun cancel()
}

class UiaStepFallbackViewModelImpl(
    viewModelContext: ViewModelContext,
    private val uiaStep: UIA.Step<*>,
    private val authenticationType: AuthenticationType,
    private val onNext: (UIA<*>) -> Unit,
    private val onCancel: () -> Unit,
    private val onError: (MatrixServerException) -> Unit,
) : ViewModelContext by viewModelContext, UiaStepFallbackViewModel {
    private val i18n = get<I18n>()
    override val waitForResult = MutableStateFlow(false)
    override val error = MutableStateFlow<String?>(null)

    private var pollingJob: Job? = null

    private val uriCaller = get<UriCaller>()
    override val fallbackUrl: String = uiaStep.getFallbackUrl(authenticationType).toString()
    override val authenticationTypeString: String = authenticationType.toString()

    override fun openFallbackUrl() {
        uriCaller(fallbackUrl, true)
        confirm()
    }

    override fun confirm() {
        if (waitForResult.getAndUpdate { true }.not()) {
            pollingJob = coroutineScope.launch {
                error.value = null
                var nextUia: UIA<*>? = null
                log.debug {
                    "nextUia.state.completed: ${(nextUia as? UIA.Step<*>)?.state?.completed ?: "[]"}, authenticationType: $authenticationType"
                }
                while (
                    nextUia !is UIA.Success<*> &&
                        ((nextUia is UIA.Step<*> && nextUia.state.completed.contains(authenticationType)).not())
                ) {
                    log.debug { "nextUia: $nextUia, authenticationType: $authenticationType" }
                    delay(POLLING_INTERVAL)
                    uiaStep
                        .authenticate(AuthenticationRequest.Fallback)
                        .onSuccess {
                            if (it is UIA.Error) {
                                log.error { "error during fallback: ${it.errorResponse.error}" }
                                error.value = i18n.uiaGenericError(it.errorResponse.error)
                            } else {
                                log.debug { "UIA fallback action was successful -> set nextUia: $it" }
                                nextUia = it
                            }
                        }
                        .onFailure { e ->
                            log.error { "error during fallback: ${e.message}" }
                            if (e is MatrixServerException) onError(e)
                            else error.value = i18n.uiaGenericError(e.message)
                            return@launch
                        }
                }
                nextUia.also {
                    log.debug { "nextUia is set -> onNext()" }
                    onNext(it)
                }
            }
            pollingJob?.invokeOnCompletion {
                log.debug { "UIA fallback action completed" }
                waitForResult.value = false
            }
        }
    }

    override fun cancel() {
        pollingJob?.cancel()
        onCancel()
    }
}

class UiaStepFallbackViewModelPreview(mode: PreviewMode = IDLE) : UiaStepFallbackViewModel {
    override val fallbackUrl: String = "https://fallbackUrl"
    override val waitForResult = MutableStateFlow(mode == AWAITING)
    override val error = MutableStateFlow(if (mode == ERROR) "Something's wrong!" else null)
    override val authenticationTypeString: String = "ReCaptcha"

    override fun openFallbackUrl() {}

    override fun confirm() {}

    override fun cancel() {}

    enum class PreviewMode {
        IDLE,
        AWAITING,
        ERROR,
    }
}
