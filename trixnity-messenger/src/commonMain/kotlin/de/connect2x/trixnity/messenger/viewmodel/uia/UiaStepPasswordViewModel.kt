package de.connect2x.trixnity.messenger.viewmodel.uia

import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModel
import de.connect2x.trixnity.messenger.viewmodel.TextFieldViewModelImpl
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepPasswordViewModelPreview.PreviewMode.BLANK
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepPasswordViewModelPreview.PreviewMode.ERROR
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepPasswordViewModelPreview.PreviewMode.FILLED
import de.connect2x.trixnity.messenger.viewmodel.uia.UiaStepPasswordViewModelPreview.PreviewMode.SUBMITTING
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.launch
import net.folivo.trixnity.clientserverapi.client.UIA
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.clientserverapi.model.uia.AuthenticationRequest
import net.folivo.trixnity.core.ErrorResponse
import net.folivo.trixnity.core.MatrixServerException
import org.koin.core.component.get


private val log = KotlinLogging.logger {}

interface UiaStepPasswordViewModelFactory {
    fun create(
        viewModelContext: ViewModelContext,
        uiaStep: UIA.Step<*>,
        onNext: (UIA<*>) -> Unit,
        onCancel: () -> Unit,
        onError: (MatrixServerException) -> Unit,
    ): UiaStepPasswordViewModel {
        return UiaStepPasswordViewModelImpl(
            viewModelContext,
            uiaStep,
            onNext,
            onCancel,
            onError,
        )
    }

    companion object : UiaStepPasswordViewModelFactory
}

interface UiaStepPasswordViewModel {
    val username: TextFieldViewModel
    val password: TextFieldViewModel
    val isSubmitting: StateFlow<Boolean>
    val error: StateFlow<String?>
    fun submit()
    fun cancel()
}

class UiaStepPasswordViewModelImpl(
    viewModelContext: ViewModelContext,
    private val uiaStep: UIA.Step<*>,
    private val onNext: (UIA<*>) -> Unit,
    private val onCancel: () -> Unit,
    private val onError: (MatrixServerException) -> Unit,
) : ViewModelContext by viewModelContext, UiaStepPasswordViewModel {
    private val i18n = get<I18n>()
    override val username = TextFieldViewModelImpl(maxLength = 1_000)
    override val password = TextFieldViewModelImpl(maxLength = 1_000)
    override val error = MutableStateFlow<String?>(null)
    override val isSubmitting: MutableStateFlow<Boolean> = MutableStateFlow(false)

    override fun submit() {
        if (isSubmitting.getAndUpdate { true }.not()) {
            coroutineScope.launch {
                error.value = null
                val authRequest = AuthenticationRequest.Password(
                    IdentifierType.User(username.textValue),
                    password.textValue,
                )
                uiaStep.authenticate(authRequest)
                    .onSuccess {
                        if (it is UIA.Error) {
                            when (val errorResponse = it.errorResponse) {
                                is ErrorResponse.Forbidden -> {
                                    log.error { "wrong password" }
                                    error.value = i18n.uiaInvalidUsernameOrPassword()
                                }

                                else -> {
                                    log.error { "error during password input: ${errorResponse.error}" }
                                    error.value = i18n.uiaGenericError(errorResponse.error)
                                }
                            }
                        } else {
                            log.debug { "UIA password action was successful -> onNext()" }
                            onNext(it)
                        }
                    }
                    .onFailure { e ->
                        log.error { "error during password input: $e" }
                        if (e is MatrixServerException) onError(e)
                        else error.value = i18n.uiaGenericError(e.message)
                    }
            }.invokeOnCompletion {
                log.debug { "UIA password action completed" }
                isSubmitting.value = false
            }
        }
    }

    override fun cancel() {
        onCancel()
    }
}

class UiaStepPasswordViewModelPreview(mode: PreviewMode = BLANK) : UiaStepPasswordViewModel {
    override val username = TextFieldViewModelImpl(maxLength = 1_000, if (mode == FILLED) "Timmy" else "")
    override val password = TextFieldViewModelImpl(maxLength = 1_000, if (mode == FILLED) "12345678" else "")
    override val error = MutableStateFlow(if (mode == ERROR) "Error!" else null)
    override val isSubmitting = MutableStateFlow(mode == SUBMITTING)
    override fun submit() {}
    override fun cancel() {}
    enum class PreviewMode {
        BLANK, FILLED, SUBMITTING, ERROR,
    }
}
