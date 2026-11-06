package de.connect2x.trixnity.messenger.multi

import android.content.Context
import de.connect2x.trixnity.messenger.util.ActivityGetter
import de.connect2x.trixnity.messenger.util.ContextGetter
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.module

suspend fun MatrixMultiMessenger.Companion.create(
    context: Context,
    coroutineContext: CoroutineContext = Dispatchers.Default,
    configuration: MatrixMultiMessengerConfiguration.() -> Unit = {},
): MatrixMultiMessenger =
    MatrixMultiMessengerImpl(coroutineContext) {
        modulesFactories += {
            module {
                single<ContextGetter> { ContextGetter { context } }
                single<ActivityGetter> { ActivityGetter(null) }
                single<CopyMultiMessengerSingletons>(named("PlatformCopyMultiMessengerSingletons")) {
                    CopyMultiMessengerSingletons { from: Scope, to: Module ->
                        to.single<ContextGetter> { from.get() }
                        to.single<ActivityGetter> { from.get() }
                    }
                }
            }
        }
        configuration()
    }
