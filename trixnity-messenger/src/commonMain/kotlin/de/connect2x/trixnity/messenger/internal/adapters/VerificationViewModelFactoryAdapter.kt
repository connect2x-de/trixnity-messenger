package de.connect2x.trixnity.messenger.internal.adapters

import de.connect2x.trixnity.client.verification.ActiveSasVerificationState
import de.connect2x.trixnity.client.verification.ActiveVerificationState
import de.connect2x.trixnity.core.model.events.m.key.verification.VerificationCancelEventContent
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.navigation.ViewModelFactoryAdapter
import de.connect2x.trixnity.messenger.internal.navigation.clear
import de.connect2x.trixnity.messenger.internal.navigation.navigationCallback
import de.connect2x.trixnity.messenger.internal.navigation.replace
import de.connect2x.trixnity.messenger.internal.routes.selfverification.RedoSelfVerificationRoute
import de.connect2x.trixnity.messenger.internal.routes.selfverification.SelfVerificationRouteMarker
import de.connect2x.trixnity.messenger.internal.routes.verification.VerificationAcceptSasStartRoute
import de.connect2x.trixnity.messenger.internal.routes.verification.VerificationCancelledRoute
import de.connect2x.trixnity.messenger.internal.routes.verification.VerificationCompareEmojisOrNumbersRoute
import de.connect2x.trixnity.messenger.internal.routes.verification.VerificationRejectedRoute
import de.connect2x.trixnity.messenger.internal.routes.verification.VerificationRequestRoute
import de.connect2x.trixnity.messenger.internal.routes.verification.VerificationRoute
import de.connect2x.trixnity.messenger.internal.routes.verification.VerificationRouteMarker
import de.connect2x.trixnity.messenger.internal.routes.verification.VerificationSelectVerificationMethodRoute
import de.connect2x.trixnity.messenger.internal.routes.verification.VerificationStepRouteMarker
import de.connect2x.trixnity.messenger.internal.routes.verification.VerificationSuccessRoute
import de.connect2x.trixnity.messenger.internal.routes.verification.VerificationTimeoutRoute
import de.connect2x.trixnity.messenger.internal.routes.verification.VerificationWaitRoute
import de.connect2x.trixnity.messenger.util.GetAccountProfileDisplayName
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.verification.v2.VerificationViewModel
import de.connect2x.trixnity.messenger.viewmodel.verification.v2.VerificationViewModelFactory
import org.koin.core.parameter.ParametersHolder

internal class VerificationViewModelFactoryAdapter(
    private val factory: VerificationViewModelFactory,
    private val routeNavigation: RouteNavigation,
    private val getAccountProfileDisplayName: GetAccountProfileDisplayName,
) : ViewModelFactoryAdapter<VerificationViewModel> {
    override fun create(parameters: ParametersHolder): VerificationViewModel {
        val route = parameters.get<VerificationRoute>()
        val viewModelContext = parameters.get<ViewModelContext>().childContext("Verification", route.userId)

        return factory.create(
            viewModelContext = viewModelContext,
            onCloseVerification = routeNavigation.navigationCallback { clear<VerificationRouteMarker>() },
            onRedoSelfVerification =
                routeNavigation.navigationCallback {
                    clear<VerificationRouteMarker>()
                    replace<SelfVerificationRouteMarker>(RedoSelfVerificationRoute(route.userId))
                },
            roomId = null,
            timelineEventId = null,
            onStepChange =
                routeNavigation.navigationCallback { newActiveVerificationState ->
                    when (newActiveVerificationState) {
                        is ActiveVerificationState.Cancel -> {
                            when (newActiveVerificationState.content.code) {
                                VerificationCancelEventContent.Code.MismatchedSas ->
                                    replace<VerificationStepRouteMarker>(VerificationRejectedRoute(route.userId))

                                VerificationCancelEventContent.Code.Timeout -> {
                                    replace<VerificationStepRouteMarker>(VerificationTimeoutRoute(route.userId))
                                }

                                else -> {
                                    replace<VerificationStepRouteMarker>(VerificationCancelledRoute(route.userId))
                                }
                            }
                        }
                        ActiveVerificationState.Done -> {
                            replace<VerificationStepRouteMarker>(VerificationSuccessRoute(route.userId))
                        }
                        is ActiveVerificationState.OwnRequest -> {
                            replace<VerificationStepRouteMarker>(VerificationWaitRoute(route.userId))
                        }
                        is ActiveVerificationState.Ready -> {
                            replace<VerificationStepRouteMarker>(
                                VerificationSelectVerificationMethodRoute(route.userId)
                            )
                        }
                        is ActiveVerificationState.Start -> {
                            // onSasStepChange needs to check the specific method
                        }
                        is ActiveVerificationState.TheirRequest -> {
                            replace<VerificationStepRouteMarker>(VerificationRequestRoute(route.userId))
                        }

                        is ActiveVerificationState.WaitForDone -> {
                            replace<VerificationStepRouteMarker>(VerificationWaitRoute(route.userId))
                        }
                        ActiveVerificationState.Undefined,
                        is ActiveVerificationState.AcceptedByOtherDevice -> {}
                    }
                },
            onSasStepChange =
                routeNavigation.navigationCallback { newActiveSasStepState ->
                    when (newActiveSasStepState) {
                        is ActiveSasVerificationState.OwnSasStart,
                        is ActiveSasVerificationState.Accept,
                        is ActiveSasVerificationState.WaitForKeys,
                        is ActiveSasVerificationState.WaitForMacs -> {
                            replace<VerificationStepRouteMarker>(VerificationWaitRoute(route.userId))
                        }

                        is ActiveSasVerificationState.TheirSasStart -> {
                            replace<VerificationStepRouteMarker>(VerificationAcceptSasStartRoute(route.userId))
                        }

                        is ActiveSasVerificationState.ComparisonByUser -> {
                            replace<VerificationStepRouteMarker>(VerificationCompareEmojisOrNumbersRoute(route.userId))
                        }
                    }
                },
            getAccountProfileDisplayName = getAccountProfileDisplayName,
        )
    }
}
