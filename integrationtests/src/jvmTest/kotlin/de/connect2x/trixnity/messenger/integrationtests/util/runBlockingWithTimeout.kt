package de.connect2x.trixnity.messenger.integrationtests.util

import io.kotest.common.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun runBlockingWithTimeout(timeout: Duration = 90.seconds, block: suspend () -> Unit) = runBlocking {
    withTimeout(timeout) {
        block()
    }
}