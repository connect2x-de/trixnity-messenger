package de.connect2x.trixnity.messenger

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.MatrixClientViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContext
import de.connect2x.trixnity.messenger.viewmodel.ViewModelContextImpl
import de.connect2x.trixnity.messenger.viewmodel.util.cancelNeverEndingCoroutines
import dev.mokkery.matcher.ArgMatchersScope
import dev.mokkery.matcher.matching
import dev.mokkery.resetAnswers
import dev.mokkery.resetCalls
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.errorCollector
import io.kotest.assertions.withClue
import io.kotest.core.names.TestName
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.spec.style.scopes.RootTestWithConfigBuilder
import io.kotest.core.spec.style.scopes.ShouldSpecContainerScope
import io.kotest.core.spec.style.scopes.TestWithConfigBuilder
import io.kotest.core.spec.style.scopes.addTest
import io.kotest.core.test.TestScope
import io.kotest.core.test.TestType
import io.kotest.core.test.config.TestConfig
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.timeout
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import org.koin.core.Koin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


private val log = KotlinLogging.logger {}

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

/**
 * Use this, when you want to use coroutine-test.
 */
fun TestScope.testViewModelContext(di: Koin) = object :
    ViewModelContext by ViewModelContextImpl(
        di = di,
        componentContext = DefaultComponentContext(LifecycleRegistry()),
        coroutineContext = coroutineContext
    ) {
    override val coroutineScope = CoroutineScope(coroutineContext)
}

/**
 * Use this, when you want to use coroutine-test.
 */
fun TestScope.testMatrixClientViewModelContext(di: Koin, userId: UserId) = object :
    MatrixClientViewModelContext by MatrixClientViewModelContextImpl(
        di = di,
        componentContext = DefaultComponentContext(LifecycleRegistry()),
        userId = userId,
        coroutineContext = coroutineContext
    ) {
    override val coroutineScope = CoroutineScope(coroutineContext)
}


// Normally one could use the context("...") method, however
// when setting the dispatcher in an outer beforeTest scope like here
// (Dispatchers.setMain(checkNotNull(currentCoroutineContext()[CoroutineDispatcher])))
// this seems to misbehave in inner contexts: https://github.com/kotest/kotest/issues/3577

/**
 * Drop in replacement for [io.kotest.core.spec.style.scopes.ShouldSpecRootScope.context]
 * since it appears to have some issues that are causing timeouts.
 *
 * Potential considerations are to not nest this function and to check whether
 * setting `coroutineTestScope` is causing any issues or possibly
 * if multiple tests in the same file have an identical name.
 */
fun ShouldSpec.shouldGroup(contextName: String, test: suspend ShouldSpecContainerScope.() -> Unit) {
    // TODO: Add optional config parameter to control test container behavior for our use cases.
    //  this would allow us to set per-testcase configs
    val config = testConfig(this)
    addTest(TestName("context ", contextName, false), false, config, TestType.Test) {
        ShouldSpecContainerScope(this).test()
    }
}

/**
 * Drop in replacement for [io.kotest.core.spec.style.scopes.ShouldSpecContainerScope.context]
 * since it appears to have some issues that are causing timeouts.
 *
 * Currently matches the implementation of ShouldSpecContainerScope.kt/context(name, test)
 */
suspend fun ShouldSpecContainerScope.shouldGroup(
    contextName: String,
    test: suspend ShouldSpecContainerScope.() -> Unit,
) {
    // TODO: Add optional config parameter to control test container behavior for our use cases.
    //  this would allow us to set per-testcase configs
    val config = testConfig(this.testScope.testCase.spec)
    registerTest(TestName(contextName), false, config, TestType.Test) {
        ShouldSpecContainerScope(this).test()
    }
}

private fun testConfig(spec: Spec): TestConfig {
    val config = TestConfig(
        timeout = spec.timeout?.milliseconds ?: 15.seconds,
//        invocationTimeout = spec.invocationTimeout?.milliseconds ?: 6.seconds,
        failfast = spec.failfast == true,
        coroutineTestScope = spec.coroutineTestScope, // ?: true,
        coroutineDebugProbes = true,
        blockingTest = true,
        threads = spec.threads ?: 1,
//        assertionMode = ,
//        assertSoftly = ,
//        concurrency = concurrency ?: 1,
    )
    return config
}

/**
 * Helper to make sure all coroutines are cancelled after the test and provide some additional logging.
 *
 * To use it with [io.kotest.core.spec.style.scopes.ShouldSpecRootScope.should], write:
 * ```
 * should("test something").withCleanup { ... }
 * ```
 */
fun RootTestWithConfigBuilder.withCleanup(test: suspend TestScope.() -> Unit): Unit =
    config { runTest(test) }

/**
 * Helper to make sure all coroutines are cancelled after the test and provide some additional logging.
 *
 * To use it with [io.kotest.core.spec.style.scopes.ShouldSpecContainerScope.should], write:
 * ```
 * context/shouldGroup("main tests"){
 *     should("test something").withCleanup { ... }
 * }
 * ```
 */
suspend fun TestWithConfigBuilder.withCleanup(test: suspend TestScope.() -> Unit): Unit =
    config { runTest(test) }

private suspend fun TestScope.runTest(test: suspend TestScope.() -> Unit) {
    val testName = "should " + testCase.name.testName
    log.debug { "- - starting test: <$testName>" }
    test()
    cancelNeverEndingCoroutines()
    log.debug { "- cancel never ending coroutines for test: <$testName>" }
}
