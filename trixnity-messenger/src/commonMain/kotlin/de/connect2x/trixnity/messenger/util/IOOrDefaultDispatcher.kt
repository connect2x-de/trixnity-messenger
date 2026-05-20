package de.connect2x.trixnity.messenger.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

expect val Dispatchers.IOOrDefault: CoroutineDispatcher
