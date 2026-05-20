package de.connect2x.trixnity.messenger.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val Dispatchers.IOOrDefault: CoroutineDispatcher
    get() = Default
