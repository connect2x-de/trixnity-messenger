package de.connect2x.trixnity.messenger

import android.content.Context
import de.connect2x.trixnity.messenger.util.ContextGetter
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module

suspend fun MatrixMessenger.Companion.create(
    context: Context,
    coroutineContext: CoroutineContext = Dispatchers.Default,
    configuration: MatrixMessengerConfiguration.() -> Unit = {},
): MatrixMessenger =
    MatrixMessengerImpl(coroutineContext) {
        modulesFactories += { module { single<ContextGetter> { ContextGetter { context } } } }
        configuration()
    }
