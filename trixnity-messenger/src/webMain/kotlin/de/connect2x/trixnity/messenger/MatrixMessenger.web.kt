package de.connect2x.trixnity.messenger

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers

suspend fun MatrixMessenger.Companion.create(
    coroutineContext: CoroutineContext = Dispatchers.Default,
    configuration: MatrixMessengerConfiguration.() -> Unit = {},
): MatrixMessenger = MatrixMessengerImpl(coroutineContext = coroutineContext, configuration = configuration)
