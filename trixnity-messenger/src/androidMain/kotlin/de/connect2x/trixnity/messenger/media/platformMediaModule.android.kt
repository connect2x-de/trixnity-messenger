package de.connect2x.trixnity.messenger.media

import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

actual fun platformMediaModule(): Module = module {
    single<MediaPlayer> { AndroidMediaPlayer(get(), get()) }.apply { bind<AutoCloseable>() }
    single<PlatformAudioRecorder> { AndroidAudioRecorder(get(), get(), get(), get(), get()) }
        .apply { bind<AutoCloseable>() }
}
