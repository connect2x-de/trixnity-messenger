package de.connect2x.trixnity.messenger

import dev.mokkery.matcher.ArgMatchersScope
import dev.mokkery.matcher.matching
import dev.mokkery.resetAnswers
import dev.mokkery.resetCalls
import io.kotest.assertions.errorCollector
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.timeout
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.core.model.RoomId
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

fun resetMocks(vararg mocks: Any) {
    resetCalls(*mocks)
    resetAnswers(*mocks)
}

inline fun <reified T : Any> ArgMatchersScope.eqNull(): T? = matching({ "eqNull" }) { it == null }

fun ArgMatchersScope.isTimelineEvent(
    thisTimelineEvent: TimelineEvent,
): TimelineEvent =
    matching({
        "isTimelineEvent(${thisTimelineEvent.eventId}"
    }) {
        it.eventId == thisTimelineEvent.eventId
    }

fun ArgMatchersScope.isRoomOf(roomId: RoomId): Room =
    matching({
        "isRoomOf($roomId)"
    }) {
        it.roomId == roomId
    }

inline fun <reified T> ArgMatchersScope.isNot(
    others: List<T>,
): T =
    matching({
        "isNot($others)"
    }) {
        others.contains(it).not()
    }
