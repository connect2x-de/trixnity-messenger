package de.connect2x.trixnity.messenger.internal.worker

import de.connect2x.trixnity.client.verification.VerificationService.SelfVerificationMethods
import de.connect2x.trixnity.core.model.UserId
import de.connect2x.trixnity.messenger.internal.logic.MatrixClientSelfVerificationMethodsLogic
import de.connect2x.trixnity.messenger.internal.navigation.InitialRoutes
import de.connect2x.trixnity.messenger.internal.navigation.Route
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.routes.AccountSetupRoute
import de.connect2x.trixnity.messenger.internal.routes.selfverification.CrossSigningBootstrapRoute
import de.connect2x.trixnity.messenger.internal.workers.CrossSigningBootstrapWorker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

class CrossSigningBootstrapWorkerTest {

    private val dummyUserId1 = UserId("user1")
    private val dummyUserId2 = UserId("user2")

    @Test
    fun `CrossSigningBootstrapWorker should update routes when inside AccountSetup`() = runTest {
        val cut = cut()

        backgroundScope.launch { cut.crossSigningBootstrapWorker.doWork() }
        testScheduler.runCurrent()
        assertEquals(listOf(), cut.routeNavigation.routes.value)

        cut.testMatrixClientSelfVerificationMethods.put(dummyUserId1, SelfVerificationMethods.NoCrossSigningEnabled)
        testScheduler.runCurrent()
        assertEquals(listOf(), cut.routeNavigation.routes.value)

        cut.testMatrixClientSelfVerificationMethods.put(dummyUserId2, SelfVerificationMethods.NoCrossSigningEnabled)
        testScheduler.runCurrent()
        assertEquals(listOf(), cut.routeNavigation.routes.value)

        cut.routeNavigation.updateNavigation { items = listOf(AccountSetupRoute(dummyUserId2)) }
        testScheduler.runCurrent()
        assertEquals(
            listOf(AccountSetupRoute(dummyUserId2), CrossSigningBootstrapRoute(dummyUserId2)),
            cut.routeNavigation.routes.value,
        )

        cut.testMatrixClientSelfVerificationMethods.remove(dummyUserId2)
        cut.routeNavigation.updateNavigation { items = emptyList() }
        testScheduler.runCurrent()
        assertEquals(listOf(), cut.routeNavigation.routes.value)

        cut.routeNavigation.updateNavigation { items = listOf(AccountSetupRoute(dummyUserId1)) }
        testScheduler.runCurrent()
        assertEquals(
            listOf(AccountSetupRoute(dummyUserId1), CrossSigningBootstrapRoute(dummyUserId1)),
            cut.routeNavigation.routes.value,
        )

        cut.testMatrixClientSelfVerificationMethods.remove(dummyUserId1)
        cut.routeNavigation.updateNavigation { items = emptyList() }
        testScheduler.runCurrent()
        assertEquals(listOf(), cut.routeNavigation.routes.value)
    }

    private fun cut(initialRoutes: List<Route> = listOf()): Cut {
        val testMatrixClientSelfVerificationMethods = TestMatrixClientSelfVerificationMethodsLogic()
        val routeNavigation = RouteNavigation(initialRoutes = InitialRoutes(initialRoutes))
        val crossSigningBootstrapWorker =
            CrossSigningBootstrapWorker(
                selfVerificationMethods = testMatrixClientSelfVerificationMethods,
                routeNavigation = routeNavigation,
            )

        return Cut(
            testMatrixClientSelfVerificationMethods = testMatrixClientSelfVerificationMethods,
            routeNavigation = routeNavigation,
            crossSigningBootstrapWorker = crossSigningBootstrapWorker,
        )
    }

    private class Cut(
        val testMatrixClientSelfVerificationMethods: TestMatrixClientSelfVerificationMethodsLogic,
        val routeNavigation: RouteNavigation,
        val crossSigningBootstrapWorker: CrossSigningBootstrapWorker,
    )

    private class TestMatrixClientSelfVerificationMethodsLogic : MatrixClientSelfVerificationMethodsLogic {

        private val selfVerificationMethods = MutableStateFlow(emptyMap<UserId, SelfVerificationMethods>())

        override fun selfVerificationMethods(): Flow<Map<UserId, SelfVerificationMethods>> {
            return selfVerificationMethods
        }

        fun remove(userId: UserId) {
            this.selfVerificationMethods.update { it - userId }
        }

        fun put(userId: UserId, selfVerificationMethods: SelfVerificationMethods) {
            this.selfVerificationMethods.update { it + (userId to selfVerificationMethods) }
        }
    }
}
