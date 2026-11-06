package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.i18n.AppLanguageUpdater
import de.connect2x.trixnity.messenger.i18n.AppLanguageUpdaterImpl
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.bind
import org.koin.dsl.module

suspend fun MatrixMessenger.Companion.create(
    coroutineContext: CoroutineContext = Dispatchers.Default,
    configuration: MatrixMessengerConfiguration.() -> Unit = {},
): MatrixMessenger =
    MatrixMessengerImpl(
        coroutineContext = coroutineContext,
        configuration = {
            modulesFactories += {
                module { single<AppLanguageUpdater> { AppLanguageUpdaterImpl(get(), get()) }.apply { bind<Worker>() } }
            }
            configuration()
        },
    )
