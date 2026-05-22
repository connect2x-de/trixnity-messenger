package de.connect2x.trixnity.messenger.integrationtests.util

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

fun runBlockingWithTimeout(timeout: Duration = 90.seconds, block: suspend CoroutineScope.() -> Unit) = runBlocking {
    withTimeout(timeout) {
        block()
        this.coroutineContext.cancelChildren()
    }
}
