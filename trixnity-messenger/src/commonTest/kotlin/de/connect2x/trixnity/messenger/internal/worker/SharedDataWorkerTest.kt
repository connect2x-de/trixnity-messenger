package de.connect2x.trixnity.messenger.internal.worker

import de.connect2x.trixnity.messenger.internal.navigation.InitialRoutes
import de.connect2x.trixnity.messenger.internal.navigation.RouteNavigation
import de.connect2x.trixnity.messenger.internal.routes.ShareDataRoute
import de.connect2x.trixnity.messenger.internal.utils.TestSharedDataHandler
import de.connect2x.trixnity.messenger.internal.workers.SharedDataWorker
import de.connect2x.trixnity.messenger.util.SharedData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class SharedDataWorkerTest {

    private val emptyPlainText = SharedData.PlainText("")

    @Test
    fun `SharedDataWorker should push route when data is shared`() = runTest {
        val cut = cut()
        assertEquals(listOf(), cut.routeNavigation.routes.value)

        cut.sharedDataHandler.onShare(emptyPlainText)
        testScheduler.runCurrent()
        assertEquals(listOf(ShareDataRoute(emptyPlainText)), cut.routeNavigation.routes.value)
    }

    @Test
    fun `SharedDataWorker should remove route when data is cleared`() = runTest {
        val cut = cut(emptyPlainText)
        assertEquals(listOf(ShareDataRoute(emptyPlainText)), cut.routeNavigation.routes.value)

        cut.sharedDataHandler.onShare(null)
        testScheduler.runCurrent()
        assertEquals(listOf(), cut.routeNavigation.routes.value)
    }

    private fun TestScope.cut(initialData: SharedData? = null): Cut {
        val sharedDataHandler = TestSharedDataHandler(initialData = initialData)
        val routeNavigation =
            RouteNavigation(
                initialRoutes = InitialRoutes(initialData?.let { listOf(ShareDataRoute(it)) } ?: emptyList())
            )
        val worker = SharedDataWorker(sharedDataHandler = sharedDataHandler, routeNavigation = routeNavigation)

        backgroundScope.launch { worker.doWork() }
        testScheduler.runCurrent()

        return Cut(sharedDataHandler = sharedDataHandler, routeNavigation = routeNavigation)
    }

    private class Cut(val sharedDataHandler: TestSharedDataHandler, val routeNavigation: RouteNavigation)
}
