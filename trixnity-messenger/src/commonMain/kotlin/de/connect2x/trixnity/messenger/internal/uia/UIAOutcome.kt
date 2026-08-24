package de.connect2x.trixnity.messenger.internal.uia

import de.connect2x.trixnity.clientserverapi.client.UIA

internal sealed interface UIAOutcome {
    data class Done(val success: UIA.Success<*>) : UIAOutcome

    data class Failed(val error: UIAError) : UIAOutcome

    data class Navigate(val destination: UIADestination) : UIAOutcome
}
