package de.connect2x.trixnity.messenger.viewmodel.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext

@OptIn(ExperimentalCoroutinesApi::class)
actual val testMainDispatcher: CoroutineDispatcher = newSingleThreadContext("main")