package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test

class UrlHandlerBaseTest {

    @Test
    fun `filter allowed host and protocol for uri`() = runTest {
        val cut = Cut("unicorn:")
        val cutState = cut.stateIn(backgroundScope, SharingStarted.Eagerly, null)
        cut.baseFlow.emit("unicorn:/dino?answer=42")
        yield()
        cutState.value shouldBe "unicorn:/dino?answer=42"
    }

    @Test
    fun `filter allowed host and protocol for url`() = runTest {
        val cut = Cut("https://connect2x.de")
        val cutState = cut.stateIn(backgroundScope, SharingStarted.Eagerly, null)
        cut.baseFlow.emit("https://connect2x.de/dino?answer=42")
        yield()
        cutState.value shouldBe "https://connect2x.de/dino?answer=42"
    }

    @Test
    fun `not filter not allowed host`() = runTest {
        val cut = Cut("https://connect2x.de")
        val cutState = cut.stateIn(backgroundScope, SharingStarted.Eagerly, null)
        cut.baseFlow.emit("https://other.com/dino?answer=42")
        yield()
        cutState.value shouldBe null
    }

    @Test
    fun `not filter not allowed protocol`() = runTest {
        val cut = Cut("unicorn:")
        val cutState = cut.stateIn(backgroundScope, SharingStarted.Eagerly, null)
        cut.baseFlow.emit("uni://dino?answer=42")
        yield()
        cutState.value shouldBe null
    }

    class Cut(appUri: String) : UriHandlerBase(
        MatrixMessengerConfiguration(appUri = appUri)
    ) {
        val baseFlow = urlHandlerFlow
    }
}
