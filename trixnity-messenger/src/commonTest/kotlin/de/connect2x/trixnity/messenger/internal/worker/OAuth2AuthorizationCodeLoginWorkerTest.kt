package de.connect2x.trixnity.messenger.internal.worker

import de.connect2x.trixnity.clientserverapi.client.oauth2.OAuth2AuthorizationCodeLoginFlow
import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolderImpl
import de.connect2x.trixnity.messenger.internal.navigation.InitialRoutes
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.routes.root.OAuth2AuthorizationCodeLoginRoute
import de.connect2x.trixnity.messenger.internal.utils.InMemorySettingsStorage
import de.connect2x.trixnity.messenger.internal.utils.TestUriHandler
import de.connect2x.trixnity.messenger.internal.workers.OAuth2AuthorizationCodeLoginWorker
import de.connect2x.trixnity.messenger.update
import de.connect2x.trixnity.messenger.viewmodel.connecting.OAuth2AuthorizationCodeLoginViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class OAuth2AuthorizationCodeLoginWorkerTest {

    private val appUriOAuth2Redirect = "oAuth2"

    private val loginState =
        MatrixMessengerSettingsBase.OAuth2LoginState(
            serverUrl = "https://matrix.dev.connect2x.de",
            type = OAuth2AuthorizationCodeLoginViewModel.Type.LOGIN,
            state =
                OAuth2AuthorizationCodeLoginFlow.AuthRequestData.State(
                    clientId = "clientId",
                    state = "state",
                    codeVerifier = "codeVerifier",
                ),
        )

    private val redirectUri = "${loginState.serverUrl}/${appUriOAuth2Redirect}"

    private val expectedRoute =
        OAuth2AuthorizationCodeLoginRoute(
            serverUrl = loginState.serverUrl,
            kind = loginState.type,
            initialState = loginState.state,
            redirectUri = redirectUri,
        )

    @Test
    fun `OAuth2AuthorizationCodeLoginWorker should update routes when receiving uri`() = runTest {
        val cut = cut()

        assertEquals(listOf(), cut.routeNavigation.routes.value)

        cut.testUriHandler.emit(redirectUri)
        testScheduler.runCurrent()
        assertEquals(listOf(), cut.routeNavigation.routes.value)

        cut.matrixMessengerSettingsHolder.update<MatrixMessengerSettingsBase> { it.copy(oAuth2LoginState = loginState) }
        testScheduler.runCurrent()
        assertEquals(listOf(expectedRoute), cut.routeNavigation.routes.value)
    }

    private suspend fun TestScope.cut(): Cut {
        val matrixMessengerConfiguration = MatrixMessengerConfiguration(appUriOAuth2Redirect = appUriOAuth2Redirect)
        val matrixMessengerSettingsHolder = MatrixMessengerSettingsHolderImpl(InMemorySettingsStorage())
        val uriHandler = TestUriHandler()
        val routeNavigation = RouteNavigation(initialRoutes = InitialRoutes(emptyList()))

        val worker =
            OAuth2AuthorizationCodeLoginWorker(
                matrixMessengerConfiguration = matrixMessengerConfiguration,
                matrixMessengerSettingsHolder = matrixMessengerSettingsHolder,
                uriHandler = uriHandler,
                routeNavigation = routeNavigation,
            )

        matrixMessengerSettingsHolder.init()
        backgroundScope.launch { worker.doWork() }
        testScheduler.runCurrent()

        return Cut(
            matrixMessengerSettingsHolder = matrixMessengerSettingsHolder,
            testUriHandler = uriHandler,
            routeNavigation = routeNavigation,
        )
    }

    private class Cut(
        val matrixMessengerSettingsHolder: MatrixMessengerSettingsHolder,
        val testUriHandler: TestUriHandler,
        val routeNavigation: RouteNavigation,
    )
}
