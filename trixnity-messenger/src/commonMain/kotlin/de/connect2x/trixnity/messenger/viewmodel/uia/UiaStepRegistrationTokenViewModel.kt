package de.connect2x.trixnity.messenger.viewmodel.uia

import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.i18n
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepRegistrationTokenViewModelPreview.PreviewMode.BLANK
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepRegistrationTokenViewModelPreview.PreviewMode.ERROR
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepRegistrationTokenViewModelPreview.PreviewMode.FILLED
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepRegistrationTokenViewModelPreview.PreviewMode.SUBMITTING
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.launch
import net.folivo.trixnity.clientserverapi.client.UIA
import net.folivo.trixnity.clientserverapi.model.uia.AuthenticationRequest
import net.folivo.trixnity.core.ErrorResponse
import net.folivo.trixnity.core.MatrixServerException


private val log = KotlinLogging.logger {}

interface UiaStepRegistrationTokenViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        uiaStep: UIA.Step<*>,
        onNext: (UIA<*>) -> Unit,
        onCancel: () -> Unit,
        onError: (MatrixServerException) -> Unit,
    ): UiaStepRegistrationTokenViewModel {
        return UiaStepRegistrationTokenViewModelImpl(
            viewModelContext,
            uiaStep,
            onNext,
            onCancel,
            onError,
        )
    }

    companion object : UiaStepRegistrationTokenViewModelFactory
}

interface UiaStepRegistrationTokenViewModel {
    val registrationToken: TextFieldViewModel
    val isSubmitting: StateFlow<Boolean>
    val error: StateFlow<String?>
    fun submit()
    fun cancel()
}

class UiaStepRegistrationTokenViewModelImpl(
    viewModelContext: ViewModelContext,
    private val uiaStep: UIA.Step<*>,
    private val onNext: (UIA<*>) -> Unit,
    private val onCancel: () -> Unit,
    private val onError: (MatrixServerException) -> Unit,
) : ViewModelContext by viewModelContext, UiaStepRegistrationTokenViewModel {
    override val registrationToken = TextFieldViewModelImpl(maxLength = 1_000)
    override val error = MutableStateFlow<String?>(null)
    override val isSubmitting: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override fun submit() {
        if (isSubmitting.getAndUpdate { true }.not()) {
            coroutineScope.launch {
                error.value = null
                val authRequest = AuthenticationRequest.RegistrationToken(registrationToken.value.text)
                uiaStep.authenticate(authRequest)
                    .onSuccess {
                        if (it is UIA.Error) {
                            when (val errorResponse = it.errorResponse) {
                                is ErrorResponse.Forbidden -> {
                                    log.error { "wrong registration token" }
                                    error.value = i18n.uiaInvalidRegistrationToken()
                                }

                                else -> {
                                    log.error { "error during registration token input: ${errorResponse.error}" }
                                    error.value = i18n.uiaGenericError(errorResponse.error)
                                }
                            }
                        } else {
                            log.debug { "UIA registration token action successful -> onNext()" }
                            onNext(it)
                        }
                    }
                    .onFailure { e ->
                        log.error { "error during registration token input: $e" }
                        if (e is MatrixServerException) onError(e)
                        else error.value = i18n.uiaGenericError(e.message)
                    }
            }.invokeOnCompletion {
                log.debug { "UIA registration token action completed" }
                isSubmitting.value = false
            }
        }
    }

    override fun cancel() {
        onCancel()
    }
}

class UiaStepRegistrationTokenViewModelPreview(mode: PreviewMode = BLANK) : UiaStepRegistrationTokenViewModel {
    override val registrationToken = TextFieldViewModelImpl(maxLength = 1_000, if (mode == FILLED) "12345678" else "")
    override val error = MutableStateFlow(if (mode == ERROR) "Error!" else null)
    override val isSubmitting = MutableStateFlow(mode == SUBMITTING)
    override fun submit() {}
    override fun cancel() {}
    enum class PreviewMode {
        BLANK, FILLED, SUBMITTING, ERROR,
    }
}
