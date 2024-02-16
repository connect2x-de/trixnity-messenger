package de.connect2x.trixnity.messenger.multi

import android.content.Context
import org.koin.dsl.module

suspend fun MatrixMultiMessenger.Companion.create(
    context: Context,
    configuration: MatrixMultiMessengerConfiguration.() -> Unit = {},
): MatrixMultiMessenger = MatrixMultiMessengerImpl {
    configuration()
    val oldMessenger = messenger
    messenger = {
        oldMessenger()
        modules += module { single<Context> { context } }
    }
    modules += module { single<Context> { context } }
}