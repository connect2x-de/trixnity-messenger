package de.connect2x.trixnity.messenger.media

import de.connect2x.trixnity.messenger.configureTestLogging
import io.kotest.assertions.fail
import io.kotest.assertions.withClue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class MediaItemLifecycleTest {

    @BeforeTest
    fun setup() {
        configureTestLogging()
    }

    @Test
    fun `should close when old lifecycle scope is cancelling and media player is ready`() = runTest {
        val cut = MockItem(backgroundScope)
        val coroutineScope = CoroutineScope(EmptyCoroutineContext)
        cut.updateLifecycle(coroutineScope)
        coroutineScope.cancel()

        withClue({ "MediaLifecycleItem was not closed" }) {
            withContext(Dispatchers.Default.limitedParallelism(1)) {
                withTimeout(1.seconds) {
                    cut.isClosed.await()
                }
            }
        }
    }

    @Test
    fun `should not close when old lifecycle scope is expiring and media player is ready`() = runTest {
        val cut = MockItem(backgroundScope)

        // First scope getting closed
        val coroutineScope1 = CoroutineScope(EmptyCoroutineContext)
        cut.updateLifecycle(coroutineScope1)

        // Second scope leaving open
        val coroutineScope2 = CoroutineScope(EmptyCoroutineContext)
        cut.updateLifecycle(coroutineScope2)
        coroutineScope1.cancel()

        // Validate
        delay(200.milliseconds)
        if (cut.isClosed.isCompleted)
            fail("MediaLifecycleItem was closed, but it shouldn't be")
    }

    @Test
    fun `should close later when lifecycle scope is expiring and media item is playing`() = runTest {
        val cut = MockItem(backgroundScope)
        cut.state.value = MediaPlayer.State.Playing

        // Attach lifecycle scope and close
        val coroutineScope = CoroutineScope(EmptyCoroutineContext)
        cut.updateLifecycle(coroutineScope)
        coroutineScope.cancel()

        // Validate
        delay(200.milliseconds)
        if (cut.isClosed.isCompleted)
            fail("MediaLifecycleItem was closed, but it shouldn't be")

        // Stop playing and validate again
        cut.state.value = MediaPlayer.State.Ready
        delay(200.milliseconds)
        withClue({ "MediaLifecycleItem was not closed" }) {
            withContext(Dispatchers.Default.limitedParallelism(1)) {
                withTimeout(1.seconds) {
                    cut.isClosed.await()
                }
            }
        }
    }

    private class MockItem(backgroundScope: CoroutineScope) : MediaLifecycleItemImpl(backgroundScope) {
        override val state: MutableStateFlow<MediaPlayer.State> = MutableStateFlow(MediaPlayer.State.Ready)
        val isClosed: CompletableDeferred<Unit> = CompletableDeferred()

        override fun close() {
            isClosed.complete(Unit)
        }
    }
}
