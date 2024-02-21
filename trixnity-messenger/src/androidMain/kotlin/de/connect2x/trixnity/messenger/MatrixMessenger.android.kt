package de.connect2x.trixnity.messenger

import android.content.Context
import org.koin.dsl.module

suspend fun MatrixMessenger.Companion.create(
    context: Context,
    configuration: MatrixMessengerConfiguration.() -> Unit = {},
): MatrixMessenger = MatrixMessengerImpl {
    configuration()
    modules += module { single<Context> { context } }
}