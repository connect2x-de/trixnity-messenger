package de.connect2x.trixnity.messenger.internal.uia

import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.clear
import de.connect2x.trixnity.messenger.internal.navigation.replace
import de.connect2x.trixnity.messenger.internal.routes.uia.UiaActionConfirmationRoute
import de.connect2x.trixnity.messenger.internal.routes.uia.UiaRouteMarker
import de.connect2x.trixnity.messenger.internal.routes.uia.UiaStepDummyRoute
import de.connect2x.trixnity.messenger.internal.routes.uia.UiaStepEmailIdentityRoute
import de.connect2x.trixnity.messenger.internal.routes.uia.UiaStepFallbackRoute
import de.connect2x.trixnity.messenger.internal.routes.uia.UiaStepMsisdnRoute
import de.connect2x.trixnity.messenger.internal.routes.uia.UiaStepPasswordRoute
import de.connect2x.trixnity.messenger.internal.routes.uia.UiaStepRegistrationTokenRoute

internal interface UIANavigator {
    suspend fun navigate(destination: UIADestination)
}

internal fun UIANavigator(routeNavigation: RouteNavigation, uiaStateHolder: UIAStateHolder): UIANavigator {
    return UIANavigatorImpl(routeNavigation = routeNavigation, uiaStateHolder = uiaStateHolder)
}

private class UIANavigatorImpl(
    private val routeNavigation: RouteNavigation,
    private val uiaStateHolder: UIAStateHolder,
) : UIANavigator {

    override suspend fun navigate(destination: UIADestination) {
        when (destination) {
            is UIADestination.Confirm -> confirm(destination)
            is UIADestination.Dummy -> dummy(destination)
            is UIADestination.Password -> password(destination)
            is UIADestination.RegistrationToken -> registrationToken(destination)
            is UIADestination.EmailIdentity -> emailIdentity(destination)
            is UIADestination.Msisdn -> msisdn(destination)
            is UIADestination.Fallback -> fallback(destination)
            is UIADestination.None -> none()
        }
    }

    private fun confirm(destination: UIADestination.Confirm) {
        uiaStateHolder.provideAction(destination.action)
        routeNavigation.updateNavigation {
            replace<UiaRouteMarker>(UiaActionConfirmationRoute(destination.confirmationMessage))
        }
    }

    private fun dummy(destination: UIADestination.Dummy) {
        uiaStateHolder.provideStep(destination.step)
        routeNavigation.updateNavigation { replace<UiaRouteMarker>(UiaStepDummyRoute) }
    }

    private fun password(destination: UIADestination.Password) {
        uiaStateHolder.provideStep(destination.step)
        routeNavigation.updateNavigation { replace<UiaRouteMarker>(UiaStepPasswordRoute) }
    }

    private fun registrationToken(destination: UIADestination.RegistrationToken) {
        uiaStateHolder.provideStep(destination.step)
        routeNavigation.updateNavigation { replace<UiaRouteMarker>(UiaStepRegistrationTokenRoute) }
    }

    private fun emailIdentity(destination: UIADestination.EmailIdentity) {
        uiaStateHolder.provideStep(destination.step)
        routeNavigation.updateNavigation { replace<UiaRouteMarker>(UiaStepEmailIdentityRoute) }
    }

    private fun msisdn(destination: UIADestination.Msisdn) {
        uiaStateHolder.provideStep(destination.step)
        routeNavigation.updateNavigation { replace<UiaRouteMarker>(UiaStepMsisdnRoute) }
    }

    private fun fallback(destination: UIADestination.Fallback) {
        uiaStateHolder.provideStep(destination.step)
        routeNavigation.updateNavigation {
            replace<UiaRouteMarker>(UiaStepFallbackRoute(destination.authenticationType))
        }
    }

    private fun none() {
        uiaStateHolder.clear()
        routeNavigation.updateNavigation { clear<UiaRouteMarker>() }
    }
}
