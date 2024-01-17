package de.connect2x.trixnity.messenger

import android.content.Context

suspend fun MatrixMessenger.Companion.create(
    context: Context,
    configuration: MatrixMessengerConfiguration.() -> Unit = {},
): MatrixMessenger = MatrixMessenger.internalCreate(configuration) {
    single<Context> { context }
}