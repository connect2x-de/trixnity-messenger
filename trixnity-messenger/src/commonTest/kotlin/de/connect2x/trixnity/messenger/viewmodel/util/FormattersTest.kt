package de.connect2x.trixnity.messenger.viewmodel.util

import dev.mokkery.matcher.*

import dev.mokkery.answering.*

import dev.mokkery.*

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class FormattersTest : ShouldSpec() {
    override fun timeout(): Long = 2_000

    val clock = mock<Clock>()

    init {
        beforeTest {

            every { clock.now() } returns Instant.parse("2022-03-10T19:00:00.000Z")
        }

        should("show the time when date is today") {
            formatTimestamp(Instant.parse("2022-03-10T03:12:00.000Z"), clock) shouldBe "04:12" // UTC -> CET
        }

        should("show the date if the date is at least from yesterday") {
            formatTimestamp(Instant.parse("2022-03-09T22:12:00.000Z"), clock) shouldBe "09.03.22"
        }
    }
}
