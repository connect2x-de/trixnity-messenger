package de.connect2x.trixnity.messenger.multi

suspend fun MatrixMultiMessenger.Companion.create(
    configuration: MatrixMultiMessengerConfiguration.() -> Unit = {},
): MatrixMultiMessenger = MatrixMultiMessengerImpl(configuration = configuration)