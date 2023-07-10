package de.connect2x.trixnity.messenger.viewmodel.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext

@OptIn(DelicateCoroutinesApi::class)
actual val testMainDispatcher: CoroutineDispatcher = newSingleThreadContext("main")