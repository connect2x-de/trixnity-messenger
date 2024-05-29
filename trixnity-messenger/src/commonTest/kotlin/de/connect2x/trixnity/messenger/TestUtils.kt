package de.connect2x.trixnity.messenger

import io.kotest.assertions.errorCollector
import io.kotest.assertions.withClue
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.timeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class)
suspend fun <T> Flow<T>.firstWithClue(duration: Duration = 1.seconds, expected: (T) -> T): T =
    withClue("waited for $duration but didn't found expected value in flow") {
        timeout(duration).first {
            val expectedValue = expected(it)
            errorCollector.pushClue { "--> expected $expectedValue\n     but was $it" }
            expectedValue == it
        }
    }

suspend fun <T> Flow<T>.firstWithClue(expectedValue: T, duration: Duration = 1.seconds): T =
    firstWithClue(duration) { expectedValue }
