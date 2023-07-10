package de.connect2x.trixnity.messenger.viewmodel.util

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlin.coroutines.CoroutineContext

actual val ioCoroutineContext: CoroutineContext = Dispatchers.Default

@OptIn(DelicateCoroutinesApi::class)
actual fun runBlocking(block: suspend () -> Unit): dynamic = GlobalScope.promise { block() }