package de.connect2x.trixnity.messenger

import io.kotest.assertions.errorCollector
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.timeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class)
suspend fun <T> Flow<T>.firstWithClue(duration: Duration = 1.seconds, expected: (T) -> T): T {
    var clueCount = 0
    return try {
        withClue("waited for $duration but didn't found expected value in flow") {
            timeout(duration).first {
                val expectedValue = expected(it)
                errorCollector.pushClue { "--> expected $expectedValue\n     but was $it" }
                clueCount++
                expectedValue == it
            }
        }
    } finally {
        repeat(clueCount) {
            errorCollector.popClue()
        }
    }
}

suspend fun <T> Flow<T>.firstWithClue(expectedValue: T, duration: Duration = 1.seconds): T =
    firstWithClue(duration) { expectedValue }

@OptIn(FlowPreview::class)
suspend fun <T> Flow<T?>.firstNotNullWithClue(duration: Duration = 1.seconds): T {
    var clueCount = 0
    return try {
        withClue("waited for $duration but didn't found non null value in flow") {
            timeout(duration).first {
                errorCollector.pushClue { "--> expected not null but was $it" }
                clueCount++
                it != null
            }.shouldNotBeNull()
        }
    } finally {
        repeat(clueCount) {
            errorCollector.popClue()
        }
    }
}
