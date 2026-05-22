package de.connect2x.trixnity.messenger.multi

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers

suspend fun MatrixMultiMessenger.Companion.create(
    coroutineContext: CoroutineContext = Dispatchers.Default,
    configuration: MatrixMultiMessengerConfiguration.() -> Unit = {},
): MatrixMultiMessenger = MatrixMultiMessengerImpl(configuration = configuration)
