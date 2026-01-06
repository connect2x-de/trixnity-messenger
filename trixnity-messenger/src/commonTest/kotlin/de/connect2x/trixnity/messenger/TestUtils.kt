package de.connect2x.trixnity.messenger

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContextImpl
import dev.mokkery.matcher.ArgMatchersScope
import dev.mokkery.matcher.matching
import dev.mokkery.resetAnswers
import dev.mokkery.resetCalls
import io.kotest.assertions.withClue
import io.kotest.matchers.errorCollector
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import org.koin.core.Koin
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class)
suspend inline fun <T> Flow<T>.firstWithClue(duration: Duration = 1.seconds, crossinline expected: (T) -> T): T {
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

suspend inline fun <T> Flow<T>.firstWithClue(expectedValue: T, duration: Duration = 1.seconds): T =
    firstWithClue(duration) { expectedValue }

@OptIn(FlowPreview::class)
suspend inline fun <T> Flow<T?>.firstNotNullWithClue(duration: Duration = 1.seconds): T {
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

fun runTestWithCoroutineScope(
    testBody: suspend TestScope.(CoroutineScope) -> Unit
): TestResult = runTest {
    val coroutineScope = CoroutineScope(coroutineContext + Job())
    try {
        testBody(coroutineScope)
    } finally {
        coroutineScope.cancel()
    }
}

fun resetMocks(vararg mocks: Any) {
    resetCalls(*mocks)
    resetAnswers(*mocks)
}

inline fun <reified T : Any> ArgMatchersScope.eqNull(): T? = matching({ "eqNull" }) { it == null }

fun ArgMatchersScope.isRoomOf(roomId: RoomId): Room = matching({
    "isRoomOf($roomId)"
}) {
    it.roomId == roomId
}

inline fun <reified T> ArgMatchersScope.isNot(
    others: List<T>,
): T = matching({
    "isNot($others)"
}) {
    others.contains(it).not()
}

fun TestScope.testViewModelContext(di: Koin) = object : ViewModelContext by ViewModelContextImpl(
    di = di,
    componentContext = DefaultComponentContext(LifecycleRegistry()),
    coroutineContext = backgroundScope.coroutineContext
) {
    override val coroutineScope = backgroundScope
}


fun TestScope.testMatrixClientViewModelContext(di: Koin, userId: UserId) =
    object : MatrixClientViewModelContext by MatrixClientViewModelContextImpl(
        di = di,
        componentContext = DefaultComponentContext(LifecycleRegistry()),
        userId = userId,
        coroutineContext = backgroundScope.coroutineContext
    ) {
        override val coroutineScope = backgroundScope
    }

suspend fun <T> continually(
    duration: Duration,
    test: suspend () -> T,
): T {
    val iterations = (duration / 25.milliseconds).toInt()

    var res: T = test()
    delay(duration)

    repeat(iterations) {
        res = test()
    }

    return res
}

suspend fun <T> eventually(
    duration: Duration,
    test: suspend () -> T,
): T {
    delay(duration)
    return test()
}


val CoroutineContext.coroutineDispatcher
    get() = this[ContinuationInterceptor] as CoroutineDispatcher

val CoroutineScope.coroutineDispatcher
    get() = coroutineContext.coroutineDispatcher

val TestScope.coroutineDispatcher
    get() = coroutineContext.coroutineDispatcher as TestDispatcher

val CoroutineContext.testDispatcher
    get() = coroutineDispatcher as TestDispatcher

val CoroutineScope.testDispatcher
    get() = coroutineDispatcher as TestDispatcher

val TestScope.testDispatcher
    get() = coroutineDispatcher

val CoroutineContext.testScheduler
    get() = testDispatcher.scheduler

val CoroutineScope.testScheduler
    get() = testDispatcher.scheduler

fun TestScope.settle() = testScheduler.runCurrent()
fun CoroutineContext.settle() = testScheduler.runCurrent()
fun CoroutineScope.settle() = testScheduler.settle()
suspend fun settle() = currentCoroutineContext().testScheduler.settle()
