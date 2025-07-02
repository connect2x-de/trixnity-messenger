package de.connect2x.trixnity.messenger

import kotlin.coroutines.CoroutineContext

suspend fun MatrixMessenger.Companion.create(
    coroutineContext: CoroutineContext,
    configuration: MatrixMessengerConfiguration.() -> Unit = {},
): MatrixMessenger = MatrixMessengerImpl(coroutineContext = coroutineContext, configuration = configuration)
