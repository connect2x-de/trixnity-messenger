package de.connect2x.trixnity.messenger.media

import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

actual fun platformMediaModule(): Module = module {
    single<MediaPlayer> { AppleMediaPlayer(get()) }.apply { bind<AutoCloseable>() }
    single<PlatformAudioRecorder> { AppleAudioRecorder(get(), get(), get()) }.apply { bind<AutoCloseable>() }
}
