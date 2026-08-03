package de.connect2x.trixnity.messenger.internal.uia

import de.connect2x.trixnity.clientserverapi.client.UIA
import de.connect2x.trixnity.clientserverapi.model.uia.AuthenticationType
import de.connect2x.trixnity.clientserverapi.model.uia.UIAState

internal interface UIALogic {
    fun next(uia: UIA<*>): UIAOutcome
}

internal fun UIALogic(): UIALogic {
    return UIALogicImpl()
}

private class UIALogicImpl : UIALogic {
    override fun next(uia: UIA<*>): UIAOutcome =
        when (uia) {
            is UIA.Success<*> -> UIAOutcome.Done(uia)
            is UIA.Error<*> -> UIAOutcome.Failed(UIAError.UNHANDLED)
            is UIA.Step<*> -> nextStep(uia)
        }

    private fun nextStep(step: UIA.Step<*>): UIAOutcome {
        val type = remainingAuthenticationType(step.state) ?: return UIAOutcome.Failed(UIAError.NO_STEP)

        val destination =
            when (type) {
                is AuthenticationType.Dummy -> UIADestination.Dummy(step)
                is AuthenticationType.Password -> UIADestination.Password(step)
                is AuthenticationType.RegistrationToken -> UIADestination.RegistrationToken(step)
                is AuthenticationType.EmailIdentity -> UIADestination.EmailIdentity(step)
                is AuthenticationType.Msisdn -> UIADestination.Msisdn(step)
                is AuthenticationType.OAuth2 -> UIADestination.Fallback(step, type)
                is AuthenticationType.Recaptcha -> UIADestination.Fallback(step, type)
                is AuthenticationType.SSO -> UIADestination.Fallback(step, type)
                is AuthenticationType.TermsOfService -> UIADestination.Fallback(step, type)
                is AuthenticationType.Unknown -> UIADestination.Fallback(step, type)
            }
        return UIAOutcome.Navigate(destination)
    }

    private fun remainingAuthenticationType(state: UIAState): AuthenticationType? {
        val preferredFlow =
            state.flows.firstOrNull { flow -> flow.stages.none { it is AuthenticationType.Unknown } }
                ?: state.flows.firstOrNull()
        return (preferredFlow?.stages.orEmpty() - state.completed.toSet()).firstOrNull()
    }
}
