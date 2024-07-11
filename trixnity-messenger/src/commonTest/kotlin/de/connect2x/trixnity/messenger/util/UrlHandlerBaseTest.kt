package de.connect2x.trixnity.messenger.util

import dev.mokkery.matcher.*

import dev.mokkery.answering.*

import de.connect2x.trixnity.messenger.MatrixMessengerConfiguration
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class UrlHandlerBaseTest : ShouldSpec({
    timeout = 5_000

    lateinit var coroutineScope: CoroutineScope
    beforeTest {
        coroutineScope = CoroutineScope(Dispatchers.Unconfined)
    }
    afterTest {
        coroutineScope.cancel()
    }
    fun createCut(urlHost: String = "dino", urlProtocol: String = "unicorn") =
        object : UrlHandlerBase(
            MatrixMessengerConfiguration(urlHost = urlHost, urlProtocol = urlProtocol)
        ) {
            val baseFlow = urlHandlerFlow
        }

    should("filter allowed host and protocol") {
        val cut = createCut()
        val cutState = cut.stateIn(coroutineScope, SharingStarted.Eagerly, null)
        cut.baseFlow.emit(Url("unicorn://dino?answer=42"))
        cutState.value?.toString() shouldBe "unicorn://dino?answer=42"
    }
    should("not filter not allowed host") {
        val cut = createCut()
        val cutState = cut.stateIn(coroutineScope, SharingStarted.Eagerly, null)
        cut.baseFlow.emit(Url("unicorn://din?answer=42"))
        cutState.value?.toString() shouldBe null
    }
    should("not filter not allowed protocol") {
        val cut = createCut()
        val cutState = cut.stateIn(coroutineScope, SharingStarted.Eagerly, null)
        cut.baseFlow.emit(Url("uni://dino?answer=42"))
        cutState.value?.toString() shouldBe null
    }
    should("allow empty host") {
        val cut = createCut("")
        val cutState = cut.stateIn(coroutineScope, SharingStarted.Eagerly, null)
        println(Url("unicorn://?answer=42").host)
        cut.baseFlow.emit(Url("unicorn://?answer=42"))
        cutState.value?.toString() shouldBe "unicorn://localhost?answer=42" // ktor does not allow empty host and defaults to UrlBuilder.origin
    }
})
