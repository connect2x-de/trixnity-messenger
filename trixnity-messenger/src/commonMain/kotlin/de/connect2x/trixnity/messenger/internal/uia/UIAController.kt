package de.connect2x.trixnity.messenger.internal.uia

import de.connect2x.trixnity.clientserverapi.client.UIA
import de.connect2x.trixnity.core.MatrixServerException
import de.connect2x.trixnity.messenger.i18n.I18n
import de.connect2x.trixnity.messenger.viewmodel.uia.AuthorizeUiaResult

internal interface UIAController {
    suspend fun next(uia: UIA<*>)

    suspend fun cancel()

    suspend fun error(exception: MatrixServerException)
}

internal fun UIAController(
    i18n: I18n,
    logic: UIALogic,
    sessions: UIASessionHolder,
    navigator: UIANavigator,
): UIAController {
    return UIAControllerImpl(i18n, logic, sessions, navigator)
}

private class UIAControllerImpl(
    private val i18n: I18n,
    private val logic: UIALogic,
    private val sessions: UIASessionHolder,
    private val navigator: UIANavigator,
) : UIAController {

    override suspend fun next(uia: UIA<*>) {
        when (val outcome = logic.next(uia)) {
            is UIAOutcome.Done -> finish(AuthorizeUiaResult.Success(outcome.success))
            is UIAOutcome.Failed ->
                finish(AuthorizeUiaResult.UnexpectedError<Nothing>(i18n.uiaGenericError(outcome.error.name)))
            is UIAOutcome.Navigate -> navigator.navigate(outcome.destination)
        }
    }

    override suspend fun cancel() {
        finish(AuthorizeUiaResult.CancelledByUser<Nothing>(i18n.uiaCancelledByUser()))
    }

    override suspend fun error(exception: MatrixServerException) {
        finish(AuthorizeUiaResult.Error<Nothing>(exception))
    }

    private fun finish(result: AuthorizeUiaResult<*>) {
        sessions.current()?.complete(result)
    }
}
