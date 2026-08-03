package de.connect2x.trixnity.messenger.internal.uia

import de.connect2x.trixnity.clientserverapi.client.UIA
import de.connect2x.trixnity.clientserverapi.model.uia.AuthenticationType

internal sealed interface UIADestination {

    data class Dummy(val step: UIA.Step<*>) : UIADestination

    data class Password(val step: UIA.Step<*>) : UIADestination

    data class RegistrationToken(val step: UIA.Step<*>) : UIADestination

    data class EmailIdentity(val step: UIA.Step<*>) : UIADestination

    data class Msisdn(val step: UIA.Step<*>) : UIADestination

    data class Fallback(val step: UIA.Step<*>, val authenticationType: AuthenticationType) : UIADestination

    data class Confirm(val confirmationMessage: String?, val action: suspend () -> Result<UIA<*>>) : UIADestination

    data object None : UIADestination
}
