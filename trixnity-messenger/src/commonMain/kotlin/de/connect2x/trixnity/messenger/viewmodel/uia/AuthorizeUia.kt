package de.connect2x.trixnity.messenger.viewmodel.uia

import de.connect2x.lognity.api.logger.Logger
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import de.connect2x.trixnity.clientserverapi.client.UIA
import de.connect2x.trixnity.core.MatrixServerException

interface AuthorizeUia {
    val onRequestFlow: Flow<AuthorizeUiaParams>

    suspend operator fun <T> invoke(
        confirmationMessage: String? = null,
        action: suspend () -> Result<UIA<T>>
    ): AuthorizeUiaResult<T>
}

class AuthorizeUiaImpl : AuthorizeUia {
    companion object {
        private val log: Logger = Logger("de.connect2x.trixnity.messenger.viewmodel.uia.AuthorizeUiaImpl")
    }

    private val _onRequestFlow = MutableSharedFlow<AuthorizeUiaParams>()
    override val onRequestFlow = _onRequestFlow.asSharedFlow()

    override suspend operator fun <T> invoke(
        confirmationMessage: String?,
        action: suspend () -> Result<UIA<T>>
    ): AuthorizeUiaResult<T> =
        channelFlow {
            val onResult: suspend (AuthorizeUiaResult<*>) -> Unit = {
                log.debug { "received uia auth request result" }
                @Suppress("UNCHECKED_CAST")
                send(it as AuthorizeUiaResult<T>)
            }
            log.debug { "send uia auth request" }
            _onRequestFlow.emit(
                AuthorizeUiaParams(
                    confirmationMessage = confirmationMessage,
                    action = action,
                    onResult = onResult,
                )
            )
            awaitCancellation()
        }.first()
}

data class AuthorizeUiaParams(
    val confirmationMessage: String? = null,
    val action: suspend () -> Result<UIA<*>>,
    val onResult: suspend (AuthorizeUiaResult<*>) -> Unit,
)

sealed interface AuthorizeUiaResult<T> {
    data class CancelledByUser<T>(val message: String) : AuthorizeUiaResult<T>
    data class Error<T>(val exception: MatrixServerException) : AuthorizeUiaResult<T>
    data class UnexpectedError<T>(val message: String) : AuthorizeUiaResult<T>
    data class Success<T>(val uia: UIA.Success<T>) : AuthorizeUiaResult<T>
}
