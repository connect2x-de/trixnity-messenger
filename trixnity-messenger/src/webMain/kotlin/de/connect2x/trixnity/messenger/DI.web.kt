package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.i18n.AppLanguageUpdater
import de.connect2x.trixnity.messenger.i18n.AppLanguageUpdaterImpl
import de.connect2x.trixnity.messenger.media.MediaPlayer
import de.connect2x.trixnity.messenger.media.WebMediaPlayer
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<AppLanguageUpdater> { AppLanguageUpdaterImpl(get(), get()) }.apply { bind<Worker>() }
    single<MediaPlayer> {
        WebMediaPlayer(get())
    }.apply {
        bind<AutoCloseable>()
    }
}
