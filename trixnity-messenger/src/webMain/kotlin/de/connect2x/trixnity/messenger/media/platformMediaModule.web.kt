package de.connect2x.trixnity.messenger.media

import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import web.audio.AudioContext

actual fun platformMediaModule(): Module = module {
    single<AudioContext> { AudioContext() }
    single<MediaPlayer> { WebMediaPlayer(get(), get()) }.apply { bind<AutoCloseable>() }
    single<PlatformAudioRecorder> { WebAudioRecorder(get(), get(), get(), get()) }.apply { bind<AutoCloseable>() }
}
