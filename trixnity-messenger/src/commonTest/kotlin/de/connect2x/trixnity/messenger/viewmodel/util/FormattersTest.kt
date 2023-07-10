package de.connect2x.trixnity.messenger.viewmodel.util

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.kodein.mock.Mock
import org.kodein.mock.Mocker

class FormattersTest : ShouldSpec() {
    override fun timeout(): Long = 2_000

    val mocker = Mocker()

    @Mock
    lateinit var clock: Clock

    init {
        beforeTest {
            mocker.reset()
            injectMocks(mocker)

            mocker.every { clock.now() } returns Instant.parse("2022-03-10T19:00:00.000Z")
        }

        should("show the time when date is today") {
            formatTimestamp(Instant.parse("2022-03-10T03:12:00.000Z"), clock) shouldBe "04:12" // UTC -> CET
        }

        should("show the date if the date is at least from yesterday") {
            formatTimestamp(Instant.parse("2022-03-09T22:12:00.000Z"), clock) shouldBe "09.03.22"
        }
    }
}