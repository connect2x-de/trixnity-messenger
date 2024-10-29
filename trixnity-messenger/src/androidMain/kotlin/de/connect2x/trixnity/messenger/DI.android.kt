package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.multi.CopyMultiMessengerSingletons
import de.connect2x.trixnity.messenger.util.ActivityGetter
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<ActivityGetter> { ActivityGetter() }
    single<CopyMultiMessengerSingletons>(named("PlatformCopyMultiMessengerSingletons")) {
        CopyMultiMessengerSingletons { from: Scope, to: Module ->
            to.single<ActivityGetter> { from.get() }
        }
    }
}
