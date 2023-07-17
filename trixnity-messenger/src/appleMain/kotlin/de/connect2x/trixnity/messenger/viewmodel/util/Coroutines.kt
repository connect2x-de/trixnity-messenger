package de.connect2x.trixnity.messenger.viewmodel.util

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

actual val ioCoroutineContext: CoroutineContext = Dispatchers.Default

actual fun runBlocking(block: suspend () -> Unit) = kotlinx.coroutines.runBlocking { block() }