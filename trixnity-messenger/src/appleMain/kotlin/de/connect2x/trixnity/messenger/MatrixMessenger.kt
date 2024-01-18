package de.connect2x.trixnity.messenger

suspend fun MatrixMessenger.Companion.create(
    configuration: MatrixMessengerConfiguration.() -> Unit = {},
): MatrixMessenger = MatrixMessenger.internalCreate(configuration)