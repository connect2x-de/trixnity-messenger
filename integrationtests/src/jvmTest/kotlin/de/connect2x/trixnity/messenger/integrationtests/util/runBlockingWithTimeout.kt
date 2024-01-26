package de.connect2x.trixnity.messenger.integrationtests.util

import io.kotest.common.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

fun runBlockingWithTimeout(timeout: Duration = 1.minutes, block: suspend () -> Unit) = runBlocking {
    withTimeout(timeout) {
        block()
    }
}