package de.connect2x.trixnity.messenger.multi

import android.content.Context
import de.connect2x.trixnity.messenger.util.ContextGetter
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext

suspend fun MatrixMultiMessenger.Companion.create(
    context: Context,
    coroutineContext: CoroutineContext = Dispatchers.Default,
    configuration: MatrixMultiMessengerConfiguration.() -> Unit = {},
): MatrixMultiMessenger = MatrixMultiMessengerImpl(coroutineContext) {
    modulesFactories += { module { single<ContextGetter> { ContextGetter { context } } } }
    configuration()
}
