package de.connect2x.trixnity.messenger.internal.logic

import de.connect2x.trixnity.clientserverapi.client.UIA
import de.connect2x.trixnity.clientserverapi.model.uia.AuthenticationType
import de.connect2x.trixnity.core.MatrixServerException
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.uia.AuthorizeUia
import de.connect2x.trixnity.messenger.viewmodel.uia.AuthorizeUiaResult
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal class UiaLogic(
    viewModelContext: ViewModelContext,
    private val i18n: I18n,
    private val authFlow: AuthorizeUia,
    private val navigateConfirm: suspend (confirmationMessage: String?, action: suspend () -> Result<UIA<*>>) -> Unit,
    private val navigateDummy: suspend (step: UIA.Step<*>) -> Unit,
    private val navigatePassword: suspend (step: UIA.Step<*>) -> Unit,
    private val navigateRegistrationToken: suspend (step: UIA.Step<*>) -> Unit,
    private val navigateEmailIdentity: suspend (step: UIA.Step<*>) -> Unit,
    private val navigateMsisdn: suspend (step: UIA.Step<*>) -> Unit,
    private val navigateFallback: suspend (step: UIA.Step<*>, authenticationType: AuthenticationType) -> Unit,
    private val navigateNone: suspend () -> Unit,
) : ViewModelContext by viewModelContext {

    private val onResult = MutableSharedFlow<AuthorizeUiaResult<*>>()

    init {
        viewModelContext.coroutineScope.launch {
            authFlow.onRequestFlow.collectLatest { params ->
                coroutineScope {
                    val result = async(start = CoroutineStart.UNDISPATCHED) { onResult.first() }
                    navigateConfirm(params.confirmationMessage, params.action)
                    params.onResult(result.await())
                    navigateNone()
                }
            }
        }
    }

    suspend fun next(uia: UIA<*>) {
        when (uia) {
            is UIA.Success -> {
                log.debug { "UIA was successful" }
                success(uia)
            }

            is UIA.Step -> {
                val nextStepAuthenticationType = nextStepAuthenticationType(uia)
                log.debug { "handle UIA step with next type: $nextStepAuthenticationType" }
                if (nextStepAuthenticationType != null) {
                    when (nextStepAuthenticationType) {
                        AuthenticationType.Dummy -> navigateDummy(uia)
                        AuthenticationType.Password -> navigatePassword(uia)
                        AuthenticationType.RegistrationToken -> navigateRegistrationToken(uia)
                        AuthenticationType.EmailIdentity -> navigateEmailIdentity(uia)
                        AuthenticationType.Msisdn -> navigateMsisdn(uia)
                        AuthenticationType.OAuth2,
                        AuthenticationType.Recaptcha,
                        AuthenticationType.SSO,
                        AuthenticationType.TermsOfService,
                        is AuthenticationType.Unknown -> navigateFallback(uia, nextStepAuthenticationType)
                    }
                } else {
                    log.error { "could not continue UIA, because no next step found" }
                    unexpectedError(i18n.uiaGenericError("INTERNAL_NO_STEP"))
                }
            }

            is UIA.Error -> {
                log.error { "could not continue UIA, because UIA.Error found -> this should be handled by view models" }
                unexpectedError(i18n.uiaGenericError("INTERNAL_UNHANDLED_ERROR"))
            }
        }
    }

    suspend fun cancel() {
        onResult.emit(AuthorizeUiaResult.CancelledByUser<Nothing>(i18n.uiaCancelledByUser()))
    }

    suspend fun error(exception: MatrixServerException) {
        onResult.emit(AuthorizeUiaResult.Error<Nothing>(exception))
    }

    private suspend fun unexpectedError(message: String) {
        onResult.emit(AuthorizeUiaResult.UnexpectedError<Nothing>(message))
    }

    private suspend fun success(uia: UIA.Success<*>) {
        onResult.emit(AuthorizeUiaResult.Success(uia))
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
}
