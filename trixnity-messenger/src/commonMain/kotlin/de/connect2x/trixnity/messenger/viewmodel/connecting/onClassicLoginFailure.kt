package de.connect2x.trixnity.messenger.viewmodel.connecting

import de.connect2x.trixnity.messenger.i18n.I18n
import kotlinx.coroutines.CancellationException
import net.folivo.trixnity.clientserverapi.client.ClassicMatrixClientAuthProviderData
import net.folivo.trixnity.core.ErrorResponse
import net.folivo.trixnity.core.MatrixServerException

inline fun Result<ClassicMatrixClientAuthProviderData>.onClassicLoginFailure(
    i18n: I18n,
    action: (message: String) -> Unit
) = onFailure { exception ->
    val message = when (exception) {
        is CancellationException -> throw exception
        is MatrixServerException -> {
            when (exception.errorResponse) {
                ErrorResponse.Forbidden -> i18n.createMatrixClientFailureInvalidAuthentication()
                ErrorResponse.UserDeactivated -> i18n.createMatrixClientFailureUserDeactivated()
                else -> i18n.createMatrixClientFailureUnknown(exception.message)
            }
        }

        else -> i18n.createMatrixClientFailureUnknown(exception.message)
    }
    action(message)
}
