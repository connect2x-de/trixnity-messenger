package de.connect2x.trixnity.messenger.viewmodel.util

import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Instant

class FormattersTest {
    val clock = mock<Clock> {
        every { now() } returns Instant.parse("2022-03-10T19:00:00.000Z")
    }

    @Test
    fun `show the time when date is today`() {
        formatTimestamp(
            Instant.parse("2022-03-10T03:12:00.000Z"),
            clock,
            TimeZone.of("CET")
        ) shouldBe "04:12" // UTC -> CET
    }

    @Test
    fun `show the date if the date is at least from yesterday`() {
        formatTimestamp(Instant.parse("2022-03-09T22:12:00.000Z"), clock, TimeZone.of("CET")) shouldBe "09.03.22"
    }
}
