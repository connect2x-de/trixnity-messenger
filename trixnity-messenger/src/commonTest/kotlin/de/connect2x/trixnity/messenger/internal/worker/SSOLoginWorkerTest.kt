package de.connect2x.trixnity.messenger.internal.worker

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsBase
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolder
import de.connect2x.trixnity.messenger.MatrixMessengerSettingsHolderImpl
import de.connect2x.trixnity.messenger.internal.navigation.InitialRoutes
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.routes.root.SSOLoginRoute
import de.connect2x.trixnity.messenger.internal.utils.InMemorySettingsStorage
import de.connect2x.trixnity.messenger.internal.utils.TestUriHandler
import de.connect2x.trixnity.messenger.internal.workers.SSOLoginWorker
import de.connect2x.trixnity.messenger.update
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class SSOLoginWorkerTest {

    private val appUriSsoRedirect = "sso"

    private val loginState =
        MatrixMessengerSettingsBase.SSOLoginState(
            serverUrl = "https://matrix.dev.connect2x.de",
            state = "state",
            providerId = "providerId",
            providerName = "providerName",
        )

    private val redirectUri = "${loginState.serverUrl}/${appUriSsoRedirect}"

    private val expectedRoute =
        SSOLoginRoute(
            serverUrl = loginState.serverUrl,
            providerId = loginState.providerId,
            providerName = loginState.providerName,
            initialState = loginState.state,
            redirectUri = redirectUri,
        )

    @Test
    fun `SSOLoginWorker should update routes when receiving uri`() = runTest {
        val cut = cut()

        assertEquals(listOf(), cut.routeNavigation.routes.value)

        cut.testUriHandler.emit(redirectUri)
        testScheduler.runCurrent()
        assertEquals(listOf(), cut.routeNavigation.routes.value)

        cut.matrixMessengerSettingsHolder.update<MatrixMessengerSettingsBase> { it.copy(ssoLoginState = loginState) }
        testScheduler.runCurrent()
        assertEquals(listOf(expectedRoute), cut.routeNavigation.routes.value)
    }

    private suspend fun TestScope.cut(): Cut {
        val matrixMessengerConfiguration = MatrixMessengerConfiguration(appUriSsoRedirect = appUriSsoRedirect)
        val matrixMessengerSettingsHolder = MatrixMessengerSettingsHolderImpl(InMemorySettingsStorage())
        val uriHandler = TestUriHandler()
        val routeNavigation = RouteNavigation(initialRoutes = InitialRoutes(emptyList()))

        val worker =
            SSOLoginWorker(
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
