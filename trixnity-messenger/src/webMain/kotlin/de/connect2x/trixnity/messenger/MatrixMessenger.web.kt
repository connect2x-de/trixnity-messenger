package de.connect2x.trixnity.messenger

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

suspend fun MatrixMessenger.Companion.create(
    coroutineContext: CoroutineContext = Dispatchers.Default,
    configuration: MatrixMessengerConfiguration.() -> Unit = {},
): MatrixMessenger = MatrixMessengerImpl(
    coroutineContext = coroutineContext,
    configuration = configuration
)
