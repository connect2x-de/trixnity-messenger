package de.connect2x.trixnity.messenger.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

actual val Dispatchers.IOOrDefault: CoroutineDispatcher
    get() = IO