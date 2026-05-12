package de.connect2x.trixnity.messenger.multi

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

suspend fun MatrixMultiMessenger.Companion.create(
    coroutineContext: CoroutineContext = Dispatchers.Default,
    configuration: MatrixMultiMessengerConfiguration.() -> Unit = {},
): MatrixMultiMessenger = MatrixMultiMessengerImpl(configuration = configuration)
