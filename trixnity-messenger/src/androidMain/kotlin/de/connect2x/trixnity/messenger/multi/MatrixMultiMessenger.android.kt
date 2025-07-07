package de.connect2x.trixnity.messenger.multi

import android.content.Context
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext

suspend fun MatrixMultiMessenger.Companion.create(
    context: Context,
    coroutineContext: CoroutineContext = Dispatchers.Default,
    configuration: MatrixMultiMessengerConfiguration.() -> Unit = {},
): MatrixMultiMessenger = MatrixMultiMessengerImpl(coroutineContext) {
    configuration()
    val oldMessenger = messenger
    messenger = {
        oldMessenger()
        modulesFactories += { module { single<Context> { context } } }
    }
    modulesFactories += { module { single<Context> { context } } }
}
