package de.connect2x.trixnity.messenger.util

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import io.kotest.matchers.shouldBe
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.origin
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test

class UrlHandlerBaseTest {

    @Test
    fun `filter allowed host and protocol`() = runTest {
        val cut = Cut()
        val cutState = cut.stateIn(backgroundScope, SharingStarted.Eagerly, null)
        cut.baseFlow.emit(Url("unicorn://dino?answer=42"))
        yield()
        cutState.value?.toString() shouldBe "unicorn://dino?answer=42"
    }

    @Test
    fun `not filter not allowed host`() = runTest {
        val cut = Cut()
        val cutState = cut.stateIn(backgroundScope, SharingStarted.Eagerly, null)
        cut.baseFlow.emit(Url("unicorn://din?answer=42"))
        yield()
        cutState.value?.toString() shouldBe null
    }

    @Test
    fun `not filter not allowed protocol`() = runTest {
        val cut = Cut()
        val cutState = cut.stateIn(backgroundScope, SharingStarted.Eagerly, null)
        cut.baseFlow.emit(Url("uni://dino?answer=42"))
        yield()
        cutState.value?.toString() shouldBe null
    }

    @Test
    fun `allow empty host`() = runTest {
        val cut = Cut("")
        val cutState = cut.stateIn(backgroundScope, SharingStarted.Eagerly, null)
        cut.baseFlow.emit(Url("unicorn://?answer=42"))
        yield()
        val origin = Url(URLBuilder.origin)
        val host = origin.host
        val port = if (origin.port == 80) "" else ":${origin.port}"
        cutState.value?.toString() shouldBe "unicorn://$host$port?answer=42"
    }

    class Cut(
        urlHost: String = "dino", urlProtocol: String = "unicorn"
    ) : UrlHandlerBase(
        MatrixMessengerConfiguration(urlHost = urlHost, urlProtocol = urlProtocol)
    ) {
        val baseFlow = urlHandlerFlow
    }
}
