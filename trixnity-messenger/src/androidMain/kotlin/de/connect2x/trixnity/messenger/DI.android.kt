package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.media.AndroidMediaPlayer
import de.connect2x.trixnity.messenger.media.MediaPlayer
import de.connect2x.trixnity.messenger.multi.CopyMultiMessengerSingletons
import de.connect2x.trixnity.messenger.util.ActivityGetter
import de.connect2x.trixnity.messenger.util.ContextGetter
import de.connect2x.trixnity.messenger.util.GetAmplitudesImpl
import de.connect2x.trixnity.messenger.viewmodel.util.GetAmplitudes
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<ActivityGetter> { ActivityGetter() }
    single<CopyMultiMessengerSingletons>(named("PlatformCopyMultiMessengerSingletons")) {
        CopyMultiMessengerSingletons { from: Scope, to: Module ->
            to.single<ContextGetter> { from.get() }
            to.single<ActivityGetter> { from.get() }
        }
    }
    single<MediaPlayer> {
        AndroidMediaPlayer(get())
    }
    single<GetAmplitudes> {
        GetAmplitudesImpl
    }
}
