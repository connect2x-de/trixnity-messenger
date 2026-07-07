package de.connect2x.trixnity.messenger

import de.connect2x.trixnity.messenger.i18n.AppLanguageUpdater
import de.connect2x.trixnity.messenger.i18n.AppLanguageUpdaterImpl
import de.connect2x.trixnity.messenger.media.MediaPlayer
import de.connect2x.trixnity.messenger.media.WebMediaPlayer
import de.connect2x.trixnity.messenger.media.PlatformAudioRecorder
import de.connect2x.trixnity.messenger.media.WebAudioRecorder
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import web.audio.AudioContext

actual fun platformModule(): Module = module {
    single<AppLanguageUpdater> { AppLanguageUpdaterImpl(get(), get()) }.apply { bind<Worker>() }
    single<AudioContext> { AudioContext() }
    single<MediaPlayer> { WebMediaPlayer(get(), get()) }.apply { bind<AutoCloseable>() }
    single<PlatformAudioRecorder> { WebAudioRecorder(get(), get(), get()) }.apply { bind<AutoCloseable>() }
}
